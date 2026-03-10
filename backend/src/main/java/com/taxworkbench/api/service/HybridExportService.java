package com.taxworkbench.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxworkbench.api.application.port.query.WorkItemQueryStore;
import com.taxworkbench.api.config.AppSettings;
import com.taxworkbench.api.dto.common.JobAcceptedResponse;
import com.taxworkbench.api.dto.export.ExportJobStatusResponse;
import com.taxworkbench.api.dto.workitem.WorkItemFilter;
import com.taxworkbench.api.exception.ResourceNotFoundException;
import com.taxworkbench.api.infrastructure.mybatis.model.WorkItemGridRow;
import com.taxworkbench.api.infrastructure.jpa.entity.WorkItemEntity;
import com.taxworkbench.api.infrastructure.jpa.repository.WorkItemJpaRepository;
import com.taxworkbench.api.model.JobStatus;
import com.taxworkbench.api.model.JobType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import jakarta.annotation.PreDestroy;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

@Service
public class HybridExportService implements ExportService {
    private final JobService jobService;
    private final WorkItemQueryStore queryStore;
    private final WorkItemJpaRepository workItemRepository;
    private final AppSettings appSettings;
    private final ObjectMapper objectMapper;
    private final ExecutorService exportExecutor = Executors.newFixedThreadPool(2);

    public HybridExportService(
            JobService jobService,
            WorkItemQueryStore queryStore,
            WorkItemJpaRepository workItemRepository,
            AppSettings appSettings,
            ObjectMapper objectMapper
    ) {
        this.jobService = jobService;
        this.queryStore = queryStore;
        this.workItemRepository = workItemRepository;
        this.appSettings = appSettings;
        this.objectMapper = objectMapper;
    }

    @Override
    public JobAcceptedResponse submitExportJob(WorkItemFilter filter, String requestId) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(filter);
        } catch (Exception e) {
            payload = "{}";
        }

        JobAcceptedResponse accepted = jobService.createJob(requestId, JobType.EXPORT, payload);
        submitExportTask(accepted.jobId(), filter);
        return accepted;
    }

    @Override
    public JobAcceptedResponse submitSelectedExportJob(List<Long> workItemIds, String requestId) {
        if (workItemIds == null || workItemIds.isEmpty()) {
            throw new IllegalArgumentException("SELECTED_EXPORT_EMPTY_IDS");
        }

        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of("workItemIds", workItemIds));
        } catch (Exception e) {
            payload = "{}";
        }

        JobAcceptedResponse accepted = jobService.createJob(requestId, JobType.EXPORT, payload);
        List<Long> normalizedIds = workItemIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        submitSelectedExportTask(accepted.jobId(), normalizedIds);
        return accepted;
    }

    @Override
    public boolean resumeJob(String jobId) {
        String payload = jobService.getPayload(jobId);
        if (payload == null || payload.isBlank()) {
            return false;
        }

        try {
            if (payload.contains("\"workItemIds\"")) {
                Map<String, List<Long>> parsed = objectMapper.readValue(payload, new TypeReference<>() {});
                List<Long> ids = parsed.get("workItemIds");
                if (ids == null) {
                    return false;
                }
                List<Long> normalizedIds = ids.stream()
                        .filter(id -> id != null && id > 0)
                        .filter(id -> id > 0)
                        .distinct()
                        .toList();
                if (normalizedIds.isEmpty()) {
                    return false;
                }
                submitSelectedExportTask(jobId, normalizedIds);
                return true;
            }

            WorkItemFilter filter = objectMapper.readValue(payload, WorkItemFilter.class);
            submitExportTask(jobId, filter);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public StreamingResponseBody exportCsv(WorkItemFilter filter) {
        return outputStream -> {
            try {
                streamCsv(null, filter, outputStream, true);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    @Override
    public StreamingResponseBody downloadCsv(String jobId) {
        ExportJobStatusResponse status = jobService.getExportStatus(jobId);
        if (status.status() != JobStatus.DONE && status.status() != JobStatus.PARTIAL_SUCCESS) {
            throw new ResourceNotFoundException("Export job not ready: " + jobId);
        }
        if (status.expiresAt() != null && status.expiresAt().isBefore(OffsetDateTime.now())) {
            throw new ResourceNotFoundException("Export job expired: " + jobId);
        }

        String filePath = jobService.getFilePath(jobId);
        if (filePath == null || filePath.isBlank()) {
            throw new ResourceNotFoundException("Export file not found: " + jobId);
        }
        Path file = Path.of(filePath);
        if (!Files.exists(file)) {
            throw new ResourceNotFoundException("Export file not found: " + jobId);
        }

        return outputStream -> {
            try (InputStream inputStream = Files.newInputStream(file)) {
                inputStream.transferTo(outputStream);
                outputStream.flush();
            }
        };
    }

    @Override
    public ExportJobStatusResponse getStatus(String jobId) {
        return jobService.getExportStatus(jobId);
    }

    private void submitExportTask(String jobId, WorkItemFilter filter) {
        try {
            exportExecutor.submit(() -> runExportJob(jobId, filter));
        } catch (RejectedExecutionException ex) {
            jobService.markFailed(jobId, "EXPORT_EXECUTOR_REJECTED");
            throw ex;
        }
    }

    private void submitSelectedExportTask(String jobId, List<Long> workItemIds) {
        try {
            exportExecutor.submit(() -> runSelectedExportJob(jobId, workItemIds));
        } catch (RejectedExecutionException ex) {
            jobService.markFailed(jobId, "EXPORT_EXECUTOR_REJECTED");
            throw ex;
        }
    }

    private void runExportJob(String jobId, WorkItemFilter filter) {
        try {
            jobService.markRunning(jobId);
            assertNotCancelled(jobId);
            Path tempFile = Files.createTempFile("taxworkbench-export-" + jobId + "-", ".csv");
            try (OutputStream out = Files.newOutputStream(tempFile)) {
                streamCsv(jobId, filter, out, false);
            }

            OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(1);
            String downloadUrl = appSettings.getRuntime().getPublicBaseUrl() + "/api/v1/exports/" + jobId + "/download";
            jobService.markDone(jobId, downloadUrl, tempFile.toString(), expiresAt);
        } catch (JobCancelledException ex) {
            jobService.markCancelled(jobId, ex.getMessage());
        } catch (Exception ex) {
            jobService.markFailed(jobId, ex.getMessage());
        }
    }

    private void runSelectedExportJob(String jobId, List<Long> workItemIds) {
        try {
            jobService.markRunning(jobId);
            assertNotCancelled(jobId);
            Path tempFile = Files.createTempFile("taxworkbench-export-selected-" + jobId + "-", ".csv");
            try (OutputStream out = Files.newOutputStream(tempFile)) {
                streamSelectedCsv(jobId, workItemIds, out);
            }

            OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(1);
            String downloadUrl = appSettings.getRuntime().getPublicBaseUrl() + "/api/v1/exports/" + jobId + "/download";
            jobService.markDone(jobId, downloadUrl, tempFile.toString(), expiresAt);
        } catch (JobCancelledException ex) {
            jobService.markCancelled(jobId, ex.getMessage());
        } catch (Exception ex) {
            jobService.markFailed(jobId, ex.getMessage());
        }
    }

    private void streamCsv(String jobId, WorkItemFilter filter, OutputStream outputStream, boolean flushEachChunk) throws Exception {
        long maxBytes = appSettings.getLimits().getDownload().getMaxBytes();
        boolean unlimited = maxBytes <= 0;
        long written = 0;

        byte[] bom = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        if (!unlimited && bom.length > maxBytes) {
            throw new IllegalStateException("DOWNLOAD_LIMIT_EXCEEDED");
        }
        outputStream.write(bom);
        written += bom.length;

        String header = "id,clientName,type,status,assignee,dueDate,memo,updatedAt\n";
        byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
        if (!unlimited && written + headerBytes.length > maxBytes) {
            throw new IllegalStateException("DOWNLOAD_LIMIT_EXCEEDED");
        }
        outputStream.write(headerBytes);
        written += headerBytes.length;

        int page = 0;
        int size = effectiveExportPageSize();
        while (true) {
            assertNotCancelled(jobId);
            List<WorkItemGridRow> rows = queryStore.search(
                    filter.clientName(),
                    filter.status() == null ? null : filter.status().name(),
                    filter.assignee(),
                    filter.dueDateFrom(),
                    filter.dueDateTo(),
                    "id",
                    "desc",
                    page * size,
                    size
            );
            if (rows.isEmpty()) {
                break;
            }

            StringBuilder chunkBuilder = new StringBuilder(rows.size() * 96);
            for (WorkItemGridRow row : rows) {
                chunkBuilder
                        .append(row.id()).append(',')
                        .append(escapeCsv(row.clientName())).append(',')
                        .append(escapeCsv(row.type())).append(',')
                        .append(escapeCsv(row.status())).append(',')
                        .append(escapeCsv(row.assignee())).append(',')
                        .append(escapeCsv(row.dueDate())).append(',')
                        .append(escapeCsv(row.memo())).append(',')
                        .append(escapeCsv(row.updatedAt())).append('\n');
            }
            byte[] bytes = chunkBuilder.toString().getBytes(StandardCharsets.UTF_8);
            if (!unlimited && written + bytes.length > maxBytes) {
                throw new IllegalStateException("DOWNLOAD_LIMIT_EXCEEDED");
            }
            outputStream.write(bytes);
            written += bytes.length;

            if (flushEachChunk) {
                outputStream.flush();
            }

            if (rows.size() < size) {
                break;
            }
            page++;
        }
        outputStream.flush();
    }

    private void streamSelectedCsv(String jobId, List<Long> workItemIds, OutputStream outputStream) throws Exception {
        long maxBytes = appSettings.getLimits().getDownload().getMaxBytes();
        boolean unlimited = maxBytes <= 0;
        long written = 0;

        byte[] bom = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        if (!unlimited && bom.length > maxBytes) {
            throw new IllegalStateException("DOWNLOAD_LIMIT_EXCEEDED");
        }
        outputStream.write(bom);
        written += bom.length;

        String header = "id,clientName,type,status,assignee,dueDate,memo,updatedAt\n";
        byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
        if (!unlimited && written + headerBytes.length > maxBytes) {
            throw new IllegalStateException("DOWNLOAD_LIMIT_EXCEEDED");
        }
        outputStream.write(headerBytes);
        written += headerBytes.length;

        final int chunkSize = effectiveSelectedExportChunkSize();
        for (int from = 0; from < workItemIds.size(); from += chunkSize) {
            assertNotCancelled(jobId);
            int to = Math.min(from + chunkSize, workItemIds.size());
            List<Long> chunkIds = workItemIds.subList(from, to);
            List<WorkItemEntity> entities = workItemRepository.findByIdIn(chunkIds);
            Map<Long, WorkItemEntity> byId = new LinkedHashMap<>();
            for (WorkItemEntity entity : entities) {
                byId.put(entity.getId(), entity);
            }

            StringBuilder chunkBuilder = new StringBuilder(chunkIds.size() * 96);
            for (Long id : chunkIds) {
                WorkItemEntity row = byId.get(id);
                if (row == null) {
                    continue;
                }
                chunkBuilder
                        .append(row.getId()).append(',')
                        .append(escapeCsv(row.getClient() == null ? null : row.getClient().getName())).append(',')
                        .append(escapeCsv(row.getType() == null ? null : row.getType().name())).append(',')
                        .append(escapeCsv(row.getStatus() == null ? null : row.getStatus().name())).append(',')
                        .append(escapeCsv(row.getAssignee())).append(',')
                        .append(escapeCsv(String.valueOf(row.getDueDate()))).append(',')
                        .append(escapeCsv(row.getMemo())).append(',')
                        .append(escapeCsv(String.valueOf(row.getUpdatedAt()))).append('\n');
            }
            byte[] bytes = chunkBuilder.toString().getBytes(StandardCharsets.UTF_8);
            if (!unlimited && written + bytes.length > maxBytes) {
                throw new IllegalStateException("DOWNLOAD_LIMIT_EXCEEDED");
            }
            outputStream.write(bytes);
            written += bytes.length;
            outputStream.flush();
        }
        outputStream.flush();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private int effectiveExportPageSize() {
        int configured = appSettings.getPerformance().getExportPageSize();
        return Math.min(20000, Math.max(500, configured));
    }

    private int effectiveSelectedExportChunkSize() {
        int configured = appSettings.getPerformance().getSelectedExportChunkSize();
        return Math.min(20000, Math.max(500, configured));
    }

    private void assertNotCancelled(String jobId) {
        if (jobId != null && jobService.isCancellationRequested(jobId)) {
            throw new JobCancelledException("JOB_CANCELLED");
        }
    }

    @PreDestroy
    void shutdownExecutor() {
        exportExecutor.shutdown();
    }
}
