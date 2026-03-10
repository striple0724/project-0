package com.taxworkbench.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxworkbench.api.application.port.command.WorkItemCommandStore;
import com.taxworkbench.api.application.port.query.WorkItemQueryStore;
import com.taxworkbench.api.config.AppSettings;
import com.taxworkbench.api.dto.client.ClientFilter;
import com.taxworkbench.api.dto.client.ClientResponse;
import com.taxworkbench.api.dto.client.CreateClientRequest;
import com.taxworkbench.api.dto.client.UpdateClientRequest;
import com.taxworkbench.api.dto.common.JobAcceptedResponse;
import com.taxworkbench.api.dto.common.PageInfo;
import com.taxworkbench.api.dto.common.PagedResponse;
import com.taxworkbench.api.dto.workitem.AuditLogResponse;
import com.taxworkbench.api.dto.workitem.BulkWorkItemInput;
import com.taxworkbench.api.dto.workitem.BulkInsertRequest;
import com.taxworkbench.api.dto.workitem.BulkCsvValidationResponse;
import com.taxworkbench.api.dto.workitem.CreateWorkItemRequest;
import com.taxworkbench.api.dto.workitem.PatchWorkItemRequest;
import com.taxworkbench.api.dto.workitem.WorkItemFilter;
import com.taxworkbench.api.dto.workitem.WorkItemResponse;
import com.taxworkbench.api.exception.ConcurrencyConflictException;
import com.taxworkbench.api.exception.ResourceNotFoundException;
import com.taxworkbench.api.infrastructure.jpa.entity.ClientEntity;
import com.taxworkbench.api.infrastructure.jpa.entity.WorkItemAuditEntity;
import com.taxworkbench.api.infrastructure.jpa.entity.WorkItemEntity;
import com.taxworkbench.api.infrastructure.jpa.repository.ClientJpaRepository;
import com.taxworkbench.api.infrastructure.jpa.repository.WorkItemAuditJpaRepository;
import com.taxworkbench.api.infrastructure.jpa.repository.WorkItemJpaRepository;
import com.taxworkbench.api.infrastructure.mybatis.model.WorkItemGridRow;
import com.taxworkbench.api.model.JobType;
import com.taxworkbench.api.model.ClientStatus;
import com.taxworkbench.api.model.ClientTier;
import com.taxworkbench.api.model.ClientType;
import com.taxworkbench.api.model.WorkStatus;
import com.taxworkbench.api.model.WorkType;
import jakarta.annotation.PreDestroy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Service
@Transactional
public class HybridWorkItemService implements WorkItemService, ClientService {
    private final JobService jobService;
    private final ObjectMapper objectMapper;
    private final WorkItemCommandStore commandStore;
    private final WorkItemQueryStore queryStore;
    private final ClientJpaRepository clientRepository;
    private final WorkItemAuditJpaRepository auditRepository;
    private final WorkItemJpaRepository workItemRepository;
    private final AppSettings appSettings;
    private final TransactionTemplate transactionTemplate;
    private final ExecutorService bulkExecutor = Executors.newFixedThreadPool(2);

    public HybridWorkItemService(
            JobService jobService,
            ObjectMapper objectMapper,
            WorkItemCommandStore commandStore,
            WorkItemQueryStore queryStore,
            ClientJpaRepository clientRepository,
            WorkItemAuditJpaRepository auditRepository,
            WorkItemJpaRepository workItemRepository,
            AppSettings appSettings,
            PlatformTransactionManager transactionManager
    ) {
        this.jobService = jobService;
        this.objectMapper = objectMapper;
        this.commandStore = commandStore;
        this.queryStore = queryStore;
        this.clientRepository = clientRepository;
        this.auditRepository = auditRepository;
        this.workItemRepository = workItemRepository;
        this.appSettings = appSettings;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<WorkItemResponse> findAll(WorkItemFilter filter, int page, int size, List<String> sort, boolean includeTotal) {
        int offset = Math.max(page, 0) * Math.max(size, 1);
        SortSpec sortSpec = parseSort(sort);

        List<WorkItemGridRow> rows = queryStore.search(
                filter.clientName(),
                filter.status() == null ? null : filter.status().name(),
                filter.assignee(),
                filter.dueDateFrom(),
                filter.dueDateTo(),
                sortSpec.field(),
                sortSpec.direction(),
                offset,
                size
        );
        long total = includeTotal
                ? queryStore.count(
                        filter.clientName(),
                        filter.status() == null ? null : filter.status().name(),
                        filter.assignee(),
                        filter.dueDateFrom(),
                        filter.dueDateTo()
                )
                : -1L;

        List<WorkItemResponse> content = rows.stream().map(this::toResponse).toList();
        int totalPages = includeTotal ? (int) Math.ceil(total / (double) Math.max(size, 1)) : -1;
        return new PagedResponse<>(content, new PageInfo(page, size, total, totalPages));
    }

    @Override
    @Transactional(readOnly = true)
    public WorkItemResponse findById(long id) {
        WorkItemEntity entity = commandStore.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkItem not found: " + id));
        return toResponse(entity);
    }

    @Override
    public WorkItemResponse create(CreateWorkItemRequest request) {
        ClientEntity client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + request.clientId()));
        validateWorkItemCreateRules(client, request.type(), request.dueDate());

        WorkItemEntity entity = new WorkItemEntity();
        entity.setClient(client);
        entity.setBizNo(client.getBizNo());
        entity.setType(request.type());
        entity.setStatus(request.status() == null ? WorkStatus.TODO : request.status());
        entity.setAssignee(request.assignee());
        entity.setDueDate(request.dueDate());
        entity.setMemo(request.memo());
        entity.setTagsJson(toJson(request.tags()));

        WorkItemEntity saved = commandStore.save(entity);
        saveAudit(saved.getId(), "CREATE", null, "CREATED", request.assignee());
        return toResponse(saved);
    }

    @Override
    public WorkItemResponse patch(long id, long version, PatchWorkItemRequest request) {
        WorkItemEntity entity = commandStore.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkItem not found: " + id));
        validateWorkItemPatchRules(entity.getClient(), request.status(), request.type());

        if (entity.getVersion() != null && !entity.getVersion().equals(version)) {
            throw new ConcurrencyConflictException(
                    "리소스가 다른 사용자에 의해 변경되었습니다.",
                    version,
                    entity.getVersion(),
                    toResponse(entity)
            );
        }

        String actor = request.assignee() == null || request.assignee().isBlank() ? "system" : request.assignee();
        if (request.status() != null && request.status() != entity.getStatus()) {
            saveAudit(id, "status", entity.getStatus().name(), request.status().name(), actor);
            entity.setStatus(request.status());
        }
        if (request.type() != null && request.type() != entity.getType()) {
            saveAudit(id, "type", entity.getType().name(), request.type().name(), actor);
            entity.setType(request.type());
        }
        if (request.dueDate() != null && !request.dueDate().equals(entity.getDueDate())) {
            saveAudit(id, "dueDate", String.valueOf(entity.getDueDate()), String.valueOf(request.dueDate()), actor);
            entity.setDueDate(request.dueDate());
        }
        if (request.assignee() != null && !request.assignee().equals(entity.getAssignee())) {
            saveAudit(id, "assignee", entity.getAssignee(), request.assignee(), actor);
            entity.setAssignee(request.assignee());
        }
        if (request.memo() != null && !request.memo().equals(entity.getMemo())) {
            saveAudit(id, "memo", entity.getMemo(), request.memo(), actor);
            entity.setMemo(request.memo());
        }
        if (request.tags() != null) {
            String nextTagsJson = toJson(request.tags());
            if (!nextTagsJson.equals(entity.getTagsJson())) {
                saveAudit(id, "tags", entity.getTagsJson(), nextTagsJson, actor);
                entity.setTagsJson(nextTagsJson);
            }
        }

        WorkItemEntity saved = commandStore.save(entity);
        return toResponse(saved);
    }

    @Override
    public void delete(long id) {
        WorkItemEntity entity = commandStore.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkItem not found: " + id));
        commandStore.deleteById(entity.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AuditLogResponse> findAuditLogs(long id, String field, String from, String to, int page, int size) {
        if (commandStore.findById(id).isEmpty()) {
            throw new ResourceNotFoundException("WorkItem not found: " + id);
        }
        Pageable pageable = PageRequest.of(page, size);
        var result = auditRepository.findByWorkItemIdOrderByChangedAtDesc(id, pageable);

        List<AuditLogResponse> rows = result.getContent().stream()
                .filter(a -> field == null || field.isBlank() || field.equalsIgnoreCase(a.getFieldName()))
                .map(a -> new AuditLogResponse(
                        a.getId(),
                        a.getWorkItemId(),
                        a.getFieldName(),
                        a.getBeforeValue(),
                        a.getAfterValue(),
                        a.getChangedBy(),
                        a.getChangedAt()
                ))
                .toList();

        return new PagedResponse<>(rows, new PageInfo(page, size, result.getTotalElements(), result.getTotalPages()));
    }

    @Override
    public JobAcceptedResponse submitBulk(BulkInsertRequest request, String requestId) {
        JobAcceptedResponse accepted = jobService.createJob(requestId, JobType.BULK_INSERT, toBulkPayload(request));
        List<BulkWorkItemInput> items = List.copyOf(request.items());
        submitBulkAfterCommit(accepted.jobId(), () -> runBulkJob(accepted.jobId(), items));
        return accepted;
    }

    @Override
    public JobAcceptedResponse submitBulkCsv(MultipartFile file, String requestId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CSV_EMPTY_FILE");
        }

        String effectiveRequestId = (requestId == null || requestId.isBlank())
                ? "bulk-file-" + System.currentTimeMillis()
                : requestId;

        JobAcceptedResponse accepted = jobService.createJob(effectiveRequestId, JobType.BULK_INSERT);

        try {
            Path tempFile = Files.createTempFile("taxworkbench-bulk-" + accepted.jobId() + "-", ".csv");
            file.transferTo(tempFile);
            submitBulkAfterCommit(accepted.jobId(), () -> runBulkCsvJob(accepted.jobId(), tempFile));
        } catch (Exception ex) {
            jobService.markFailed(accepted.jobId(), ex.getMessage());
            throw new IllegalStateException("BULK_FILE_UPLOAD_FAILED", ex);
        }

        return accepted;
    }

    @Override
    @Transactional(readOnly = true)
    public BulkCsvValidationResponse validateBulkCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CSV_EMPTY_FILE");
        }

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("taxworkbench-bulk-validate-", ".csv");
            file.transferTo(tempFile);
            try {
                return validateCsvWithCharset(tempFile, StandardCharsets.UTF_8);
            } catch (MalformedInputException ex) {
                return validateCsvWithCharset(tempFile, Charset.forName("MS949"));
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("BULK_FILE_UPLOAD_FAILED", ex);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    public StreamingResponseBody downloadBulkFailureReport(String jobId) {
        String filePath = jobService.getFilePath(jobId);
        if (filePath == null || filePath.isBlank()) {
            throw new ResourceNotFoundException("Bulk failure report not found: " + jobId);
        }

        Path file = Path.of(filePath);
        if (!Files.exists(file)) {
            throw new ResourceNotFoundException("Bulk failure report not found: " + jobId);
        }

        return outputStream -> {
            try (InputStream inputStream = Files.newInputStream(file)) {
                inputStream.transferTo(outputStream);
                outputStream.flush();
            }
        };
    }

    private void runBulkJob(String jobId, List<BulkWorkItemInput> items) {
        try {
            jobService.markRunning(jobId);
            assertNotCancelled(jobId);

            Set<Long> clientIds = new HashSet<>();
            for (BulkWorkItemInput item : items) {
                clientIds.add(item.clientId());
            }

            Map<Long, ClientEntity> clientsById = new HashMap<>();
            for (ClientEntity client : clientRepository.findAllById(clientIds)) {
                clientsById.put(client.getId(), client);
            }

            int chunkSize = effectiveBulkChunkSize();
            for (int from = 0; from < items.size(); from += chunkSize) {
                assertNotCancelled(jobId);
                int to = Math.min(from + chunkSize, items.size());
                List<BulkWorkItemInput> chunk = items.subList(from, to);
                processBulkChunk(chunk, clientsById);
            }

            jobService.markDone(jobId, null, null, OffsetDateTime.now().plusHours(1));
        } catch (JobCancelledException ex) {
            jobService.markCancelled(jobId, ex.getMessage());
        } catch (Exception ex) {
            jobService.markFailed(jobId, ex.getMessage());
        }
    }

    private void runBulkCsvJob(String jobId, Path csvFile) {
        Path reportFile = null;
        try {
            jobService.markRunning(jobId);
            assertNotCancelled(jobId);
            reportFile = Files.createTempFile("taxworkbench-bulk-failures-" + jobId + "-", ".csv");
            CsvProcessResult result;
            try {
                result = processCsvWithCharset(jobId, csvFile, reportFile, StandardCharsets.UTF_8);
            } catch (MalformedInputException ex) {
                result = processCsvWithCharset(jobId, csvFile, reportFile, Charset.forName("MS949"));
            }

            OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(1);
            if (result.failureRows() > 0) {
                String downloadUrl = appSettings.getRuntime().getPublicBaseUrl() + "/api/v1/work-items/bulk-jobs/" + jobId + "/failures";
                String summary = "PARTIAL_SUCCESS:TOTAL=" + result.totalRows()
                        + ",SUCCESS=" + result.successRows()
                        + ",FAILED=" + result.failureRows();
                if (!result.reasonCounts().isEmpty()) {
                    Map.Entry<String, Integer> topReason = result.reasonCounts().entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .orElse(null);
                    if (topReason != null) {
                        summary += ",TOP_REASON=" + topReason.getKey() + "(" + topReason.getValue() + ")";
                    }
                }
                jobService.markPartialSuccess(jobId, downloadUrl, reportFile.toString(), expiresAt, summary);
            } else {
                if (reportFile != null) {
                    Files.deleteIfExists(reportFile);
                    reportFile = null;
                }
                jobService.markDone(jobId, null, null, expiresAt);
            }
        } catch (JobCancelledException ex) {
            jobService.markCancelled(jobId, ex.getMessage());
            if (reportFile != null) {
                try {
                    Files.deleteIfExists(reportFile);
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ex) {
            jobService.markFailed(jobId, ex.getMessage());
            if (reportFile != null) {
                try {
                    Files.deleteIfExists(reportFile);
                } catch (Exception ignored) {
                }
            }
        } finally {
            try {
                Files.deleteIfExists(csvFile);
            } catch (Exception ignored) {
            }
        }
    }

    private void submitBulkAfterCommit(String jobId, Runnable task) {
        Runnable submitTask = () -> {
            try {
                bulkExecutor.submit(task);
            } catch (RejectedExecutionException ex) {
                jobService.markFailed(jobId, "BULK_EXECUTOR_REJECTED");
            } catch (Exception ex) {
                jobService.markFailed(jobId, ex.getMessage());
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    submitTask.run();
                }
            });
            return;
        }

        submitTask.run();
    }

    private CsvProcessResult processCsvWithCharset(String jobId, Path csvFile, Path reportFile, Charset charset) throws Exception {
        int chunkSize = effectiveBulkChunkSize();
        List<CsvParsedItem> chunk = new ArrayList<>(chunkSize);
        String requestIdInFile = null;
        int totalRows = 0;
        int successRows = 0;
        int failureRows = 0;
        Map<String, Integer> reasonCounts = new HashMap<>();

        try (InputStream in = Files.newInputStream(csvFile);
             BufferedReader reader = new BufferedReader(new InputStreamReader(
                     in,
                     charset.newDecoder()
                             .onMalformedInput(CodingErrorAction.REPORT)
                             .onUnmappableCharacter(CodingErrorAction.REPORT)
             ));
             BufferedWriter reportWriter = Files.newBufferedWriter(reportFile, StandardCharsets.UTF_8)) {
            reportWriter.write("\uFEFFlineNo,reason,clientId,type,assignee,dueDate,tags,memo,rowData\n");

            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV_EMPTY_FILE");
            }

            CsvHeaderInfo headerInfo = readCsvHeader(headerLine);
            final boolean hasRequestIdColumn = headerInfo.hasRequestIdColumn();

            String line;
            int lineNo = 1;
            while ((line = reader.readLine()) != null) {
                assertNotCancelled(jobId);
                lineNo++;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                totalRows++;

                List<String> cols = parseCsvColumns(line);
                int expectedColumnCount = hasRequestIdColumn ? 7 : 6;
                if (cols.size() != expectedColumnCount) {
                    incrementReason(reasonCounts, "CSV_INVALID_COLUMN_COUNT");
                    writeFailureRow(reportWriter, lineNo, "CSV_INVALID_COLUMN_COUNT", null, null, null, null, null, null, line);
                    failureRows++;
                    continue;
                }

                int startIndex = 0;
                if (hasRequestIdColumn) {
                    String rowRequestId = cols.get(0).trim();
                    if (rowRequestId.isEmpty()) {
                        incrementReason(reasonCounts, "CSV_EMPTY_REQUEST_ID");
                        writeFailureRow(
                                reportWriter, lineNo, "CSV_EMPTY_REQUEST_ID",
                                cols.get(1).trim(), cols.get(2).trim(), cols.get(3).trim(), cols.get(4).trim(), cols.get(5).trim(), cols.get(6).trim(), line
                        );
                        failureRows++;
                        continue;
                    }
                    if (requestIdInFile == null) {
                        requestIdInFile = rowRequestId;
                    } else if (!requestIdInFile.equals(rowRequestId)) {
                        incrementReason(reasonCounts, "CSV_MIXED_REQUEST_ID");
                        writeFailureRow(
                                reportWriter, lineNo, "CSV_MIXED_REQUEST_ID",
                                cols.get(1).trim(), cols.get(2).trim(), cols.get(3).trim(), cols.get(4).trim(), cols.get(5).trim(), cols.get(6).trim(), line
                        );
                        failureRows++;
                        continue;
                    }
                    startIndex = 1;
                }

                String rawClientId = cols.get(startIndex).trim();
                String rawType = cols.get(startIndex + 1).trim();
                String rawAssignee = cols.get(startIndex + 2).trim();
                String rawDueDate = cols.get(startIndex + 3).trim();
                String rawTags = cols.get(startIndex + 4).trim();
                String rawMemo = cols.get(startIndex + 5).trim();

                long clientId;
                try {
                    clientId = Long.parseLong(rawClientId);
                } catch (Exception ex) {
                    incrementReason(reasonCounts, "CSV_INVALID_CLIENT_ID");
                    writeFailureRow(reportWriter, lineNo, "CSV_INVALID_CLIENT_ID", rawClientId, rawType, rawAssignee, rawDueDate, rawTags, rawMemo, line);
                    failureRows++;
                    continue;
                }

                WorkType type;
                try {
                    type = WorkType.valueOf(rawType.toUpperCase());
                } catch (Exception ex) {
                    incrementReason(reasonCounts, "CSV_INVALID_WORK_TYPE");
                    writeFailureRow(reportWriter, lineNo, "CSV_INVALID_WORK_TYPE", String.valueOf(clientId), rawType, rawAssignee, rawDueDate, rawTags, rawMemo, line);
                    failureRows++;
                    continue;
                }

                String assignee = rawAssignee;
                if (assignee.isEmpty()) {
                    incrementReason(reasonCounts, "CSV_EMPTY_ASSIGNEE");
                    writeFailureRow(reportWriter, lineNo, "CSV_EMPTY_ASSIGNEE", String.valueOf(clientId), rawType, rawAssignee, rawDueDate, rawTags, rawMemo, line);
                    failureRows++;
                    continue;
                }

                LocalDate dueDate;
                try {
                    dueDate = LocalDate.parse(rawDueDate);
                } catch (Exception ex) {
                    incrementReason(reasonCounts, "CSV_INVALID_DUEDATE");
                    writeFailureRow(reportWriter, lineNo, "CSV_INVALID_DUEDATE", String.valueOf(clientId), rawType, rawAssignee, rawDueDate, rawTags, rawMemo, line);
                    failureRows++;
                    continue;
                }

                List<String> tags = List.of();
                String tagRaw = rawTags;
                if (!tagRaw.isEmpty()) {
                    tags = java.util.Arrays.stream(tagRaw.split("\\|"))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList();
                }

                String memo = rawMemo;

                BulkWorkItemInput item = new BulkWorkItemInput(clientId, type, assignee, dueDate, tags, memo.isEmpty() ? null : memo);
                chunk.add(new CsvParsedItem(item, lineNo, line));

                if (chunk.size() >= chunkSize) {
                    assertNotCancelled(jobId);
                    CsvChunkResult chunkResult = processCsvChunkWithLookup(chunk, reportWriter);
                    successRows += chunkResult.successRows();
                    failureRows += chunkResult.failureRows();
                    mergeReasonCounts(reasonCounts, chunkResult.reasonCounts());
                    chunk.clear();
                }
            }

            if (!chunk.isEmpty()) {
                assertNotCancelled(jobId);
                CsvChunkResult chunkResult = processCsvChunkWithLookup(chunk, reportWriter);
                successRows += chunkResult.successRows();
                failureRows += chunkResult.failureRows();
                mergeReasonCounts(reasonCounts, chunkResult.reasonCounts());
            }
            reportWriter.flush();
            return new CsvProcessResult(totalRows, successRows, failureRows, Map.copyOf(reasonCounts));
        }
    }

    private CsvChunkResult processCsvChunkWithLookup(List<CsvParsedItem> chunk, BufferedWriter reportWriter) throws Exception {
        Set<Long> clientIds = new HashSet<>();
        for (CsvParsedItem row : chunk) {
            clientIds.add(row.item().clientId());
        }
        Map<Long, ClientEntity> clientsById = new HashMap<>();
        for (ClientEntity client : clientRepository.findAllById(clientIds)) {
            clientsById.put(client.getId(), client);
        }

        List<CsvParsedItem> validRows = new ArrayList<>(chunk.size());
        int failed = 0;
        Map<String, Integer> reasonCounts = new HashMap<>();
        for (CsvParsedItem row : chunk) {
            if (!clientsById.containsKey(row.item().clientId())) {
                incrementReason(reasonCounts, "CLIENT_NOT_FOUND");
                writeFailureRow(
                        reportWriter,
                        row.lineNo(),
                        "CLIENT_NOT_FOUND",
                        String.valueOf(row.item().clientId()),
                        row.item().type().name(),
                        row.item().assignee(),
                        String.valueOf(row.item().dueDate()),
                        row.item().tags() == null ? null : String.join("|", row.item().tags()),
                        row.item().memo(),
                        row.rawLine()
                );
                failed++;
                continue;
            }
            validRows.add(row);
        }

        if (validRows.isEmpty()) {
            return new CsvChunkResult(0, failed, reasonCounts);
        }

        List<BulkWorkItemInput> validItems = validRows.stream().map(CsvParsedItem::item).toList();
        processBulkChunk(validItems, clientsById);
        return new CsvChunkResult(validItems.size(), failed, reasonCounts);
    }

    private void processBulkChunk(List<BulkWorkItemInput> chunk, Map<Long, ClientEntity> clientsById) {
        transactionTemplate.executeWithoutResult(status -> {
            List<WorkItemEntity> entities = new ArrayList<>(chunk.size());

            for (BulkWorkItemInput item : chunk) {
                ClientEntity client = clientsById.get(item.clientId());
                if (client == null) {
                    throw new ResourceNotFoundException("Client not found: " + item.clientId());
                }

                WorkItemEntity entity = new WorkItemEntity();
                entity.setClient(client);
                entity.setBizNo(client.getBizNo());
                entity.setType(item.type());
                entity.setStatus(WorkStatus.TODO);
                entity.setAssignee(item.assignee());
                entity.setDueDate(item.dueDate());
                entity.setMemo(item.memo());
                entity.setTagsJson(toJson(item.tags()));
                entities.add(entity);
            }

            List<WorkItemEntity> savedEntities = workItemRepository.saveAll(entities);
            List<WorkItemAuditEntity> audits = new ArrayList<>(savedEntities.size());

            for (int i = 0; i < savedEntities.size(); i++) {
                WorkItemEntity saved = savedEntities.get(i);
                BulkWorkItemInput item = chunk.get(i);
                WorkItemAuditEntity audit = new WorkItemAuditEntity();
                audit.setWorkItemId(saved.getId());
                audit.setFieldName("CREATE");
                audit.setBeforeValue(null);
                audit.setAfterValue("CREATED");
                audit.setChangedBy(item.assignee() == null || item.assignee().isBlank() ? "system" : item.assignee());
                audits.add(audit);
            }

            auditRepository.saveAll(audits);
        });
    }

    private void processBulkChunkWithLookup(List<BulkWorkItemInput> chunk) {
        Set<Long> clientIds = new HashSet<>();
        for (BulkWorkItemInput item : chunk) {
            clientIds.add(item.clientId());
        }
        Map<Long, ClientEntity> clientsById = new HashMap<>();
        for (ClientEntity client : clientRepository.findAllById(clientIds)) {
            clientsById.put(client.getId(), client);
        }
        processBulkChunk(chunk, clientsById);
    }

    private void writeFailureRow(
            BufferedWriter writer,
            int lineNo,
            String reason,
            String clientId,
            String type,
            String assignee,
            String dueDate,
            String tags,
            String memo,
            String rawLine
    ) throws Exception {
        writer.write(lineNo + ","
                + csvCell(reason) + ","
                + csvCell(clientId) + ","
                + csvCell(type) + ","
                + csvCell(assignee) + ","
                + csvCell(dueDate) + ","
                + csvCell(tags) + ","
                + csvCell(memo) + ","
                + csvCell(rawLine)
                + "\n");
    }

    private BulkCsvValidationResponse validateCsvWithCharset(Path csvFile, Charset charset) throws Exception {
        int totalRows = 0;
        int malformedRows = 0;
        Map<Long, Integer> clientRowCounts = new HashMap<>();

        try (InputStream in = Files.newInputStream(csvFile);
             BufferedReader reader = new BufferedReader(new InputStreamReader(
                     in,
                     charset.newDecoder()
                             .onMalformedInput(CodingErrorAction.REPORT)
                             .onUnmappableCharacter(CodingErrorAction.REPORT)
             ))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("CSV_EMPTY_FILE");
            }

            CsvHeaderInfo headerInfo = readCsvHeader(headerLine);
            int expectedColumnCount = headerInfo.hasRequestIdColumn() ? 7 : 6;
            int startIndex = headerInfo.hasRequestIdColumn() ? 1 : 0;

            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                totalRows++;

                List<String> cols = parseCsvColumns(line);
                if (cols.size() != expectedColumnCount) {
                    malformedRows++;
                    continue;
                }

                if (headerInfo.hasRequestIdColumn() && cols.get(0).trim().isEmpty()) {
                    malformedRows++;
                    continue;
                }

                long clientId;
                try {
                    clientId = Long.parseLong(cols.get(startIndex).trim());
                } catch (Exception ex) {
                    malformedRows++;
                    continue;
                }
                clientRowCounts.merge(clientId, 1, Integer::sum);
            }
        }

        Set<Long> existingIds = new HashSet<>();
        for (ClientEntity client : clientRepository.findAllById(clientRowCounts.keySet())) {
            existingIds.add(client.getId());
        }

        List<Long> missingClientIds = clientRowCounts.keySet().stream()
                .filter(id -> !existingIds.contains(id))
                .sorted()
                .toList();
        int missingRows = missingClientIds.stream()
                .mapToInt(id -> clientRowCounts.getOrDefault(id, 0))
                .sum();

        int invalidRows = malformedRows + missingRows;
        int validRows = Math.max(0, totalRows - invalidRows);

        List<Long> sampleMissingIds = missingClientIds.size() > 100
                ? missingClientIds.subList(0, 100)
                : missingClientIds;

        return new BulkCsvValidationResponse(
                totalRows,
                validRows,
                invalidRows,
                clientRowCounts.size(),
                missingClientIds.size(),
                sampleMissingIds,
                malformedRows
        );
    }

    private CsvHeaderInfo readCsvHeader(String headerLine) {
        List<String> headerCols = parseCsvColumns(headerLine.replace("\uFEFF", ""));
        if (headerCols.stream().allMatch(String::isEmpty)) {
            throw new IllegalArgumentException("CSV_INVALID_HEADER");
        }

        List<String> normalizedHeader = headerCols.stream().map(v -> v.trim().toLowerCase()).toList();
        List<String> expectedHeaderWithRequestId = List.of("requestid", "clientid", "type", "assignee", "duedate", "tags", "memo");
        List<String> expectedHeaderWithoutRequestId = List.of("clientid", "type", "assignee", "duedate", "tags", "memo");

        if (normalizedHeader.equals(expectedHeaderWithRequestId)) {
            return new CsvHeaderInfo(true);
        }
        if (normalizedHeader.equals(expectedHeaderWithoutRequestId)) {
            return new CsvHeaderInfo(false);
        }
        throw new IllegalArgumentException("CSV_INVALID_HEADER");
    }

    private void incrementReason(Map<String, Integer> reasonCounts, String reason) {
        reasonCounts.merge(reason, 1, Integer::sum);
    }

    private void mergeReasonCounts(Map<String, Integer> target, Map<String, Integer> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            target.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
    }

    private String csvCell(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private List<String> parseCsvColumns(String line) {
        List<String> cols = new ArrayList<>(8);
        StringBuilder token = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    token.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }

            if (ch == ',' && !inQuotes) {
                cols.add(token.toString());
                token.setLength(0);
                continue;
            }

            token.append(ch);
        }

        if (inQuotes) {
            return List.of();
        }
        cols.add(token.toString());
        return cols;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ClientResponse> findAll(ClientFilter filter, int page, int size) {
        Specification<ClientEntity> spec = (root, query, cb) -> cb.conjunction();
        if (filter != null) {
            if (filter.name() != null && !filter.name().isBlank()) {
                spec = spec.and((root, query, cb) -> cb.like(root.get("name"), "%" + filter.name() + "%"));
            }
            if (filter.type() != null) {
                spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), filter.type()));
            }
            if (filter.tier() != null) {
                spec = spec.and((root, query, cb) -> cb.equal(root.get("tier"), filter.tier()));
            }
            if (filter.status() != null) {
                spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), filter.status()));
            }
            if (filter.createdAtFrom() != null && !filter.createdAtFrom().isBlank()) {
                spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), LocalDate.parse(filter.createdAtFrom()).atStartOfDay()));
            }
            if (filter.createdAtTo() != null && !filter.createdAtTo().isBlank()) {
                spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), LocalDate.parse(filter.createdAtTo()).atTime(23, 59, 59)));
            }
        }

        var pageResult = clientRepository.findAll(spec, PageRequest.of(page, size));
        List<ClientResponse> clients = pageResult.getContent().stream()
                .map(c -> new ClientResponse(c.getId(), c.getName(), c.getBizNo(), c.getType(), c.getStatus(), c.getTier()))
                .toList();

        return new PagedResponse<>(clients, new PageInfo(page, size, pageResult.getTotalElements(), pageResult.getTotalPages()));
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponse findClientById(long id) {
        ClientEntity entity = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + id));
        return new ClientResponse(entity.getId(), entity.getName(), entity.getBizNo(), entity.getType(), entity.getStatus(), entity.getTier());
    }

    @Override
    public ClientResponse create(CreateClientRequest request) {
        ClientEntity entity = new ClientEntity();
        entity.setName(request.name());
        entity.setBizNo(request.bizNo());
        entity.setType(request.type());
        entity.setStatus(request.status());
        entity.setTier(request.tier());
        ClientEntity saved = clientRepository.save(entity);
        return new ClientResponse(saved.getId(), saved.getName(), saved.getBizNo(), saved.getType(), saved.getStatus(), saved.getTier());
    }

    @Override
    public ClientResponse updateClient(long id, UpdateClientRequest request) {
        ClientEntity entity = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + id));

        if (request.type() == ClientType.INDIVIDUAL && workItemRepository.existsByClientIdAndType(id, WorkType.REVIEW)) {
            throw new IllegalArgumentException("CLIENT_TYPE_CONFLICT_WITH_WORK_ITEMS");
        }

        entity.setName(request.name());
        entity.setBizNo(request.bizNo());
        entity.setType(request.type());
        entity.setStatus(request.status());
        entity.setTier(request.tier());
        ClientEntity saved = clientRepository.save(entity);

        if (saved.getStatus() == ClientStatus.INACTIVE) {
            List<WorkItemEntity> activeWorkItems = workItemRepository.findByClientIdAndStatusIn(
                    saved.getId(),
                    List.of(WorkStatus.TODO, WorkStatus.IN_PROGRESS)
            );
            for (WorkItemEntity workItem : activeWorkItems) {
                if (workItem.getStatus() != WorkStatus.HOLD) {
                    saveAudit(workItem.getId(), "status", workItem.getStatus().name(), WorkStatus.HOLD.name(), "system");
                    workItem.setStatus(WorkStatus.HOLD);
                    commandStore.save(workItem);
                }
            }
        }

        return new ClientResponse(saved.getId(), saved.getName(), saved.getBizNo(), saved.getType(), saved.getStatus(), saved.getTier());
    }

    @Override
    public void deleteClient(long id) {
        if (!clientRepository.existsById(id)) {
            throw new ResourceNotFoundException("Client not found: " + id);
        }
        if (workItemRepository.existsByClientId(id)) {
            throw new IllegalArgumentException("CLIENT_HAS_WORK_ITEMS");
        }
        clientRepository.deleteById(id);
    }

    private void saveAudit(Long workItemId, String field, String beforeValue, String afterValue, String changedBy) {
        WorkItemAuditEntity audit = new WorkItemAuditEntity();
        audit.setWorkItemId(workItemId);
        audit.setFieldName(field);
        audit.setBeforeValue(beforeValue);
        audit.setAfterValue(afterValue);
        audit.setChangedBy(changedBy == null || changedBy.isBlank() ? "system" : changedBy);
        auditRepository.save(audit);
    }

    private WorkItemResponse toResponse(WorkItemEntity entity) {
        return new WorkItemResponse(
                entity.getId(),
                entity.getClient().getId(),
                entity.getClient().getName(),
                entity.getBizNo(),
                entity.getType(),
                entity.getStatus(),
                entity.getAssignee(),
                entity.getDueDate(),
                parseTags(entity.getTagsJson()),
                entity.getMemo(),
                entity.getVersion(),
                entity.getUpdatedAt(),
                auditRepository.existsByWorkItemId(entity.getId())
        );
    }

    private WorkItemResponse toResponse(WorkItemGridRow row) {
        return new WorkItemResponse(
                row.id(),
                row.clientId(),
                row.clientName(),
                row.bizNo(),
                parseWorkType(row.type()),
                parseWorkStatus(row.status()),
                row.assignee(),
                LocalDate.parse(row.dueDate()),
                parseTags(row.tagsJson()),
                row.memo(),
                row.version(),
                OffsetDateTime.parse(row.updatedAt()),
                Boolean.TRUE.equals(row.hasAudit())
        );
    }

    private WorkStatus parseWorkStatus(String value) {
        return value == null ? WorkStatus.TODO : WorkStatus.valueOf(value);
    }

    private WorkType parseWorkType(String value) {
        return value == null ? WorkType.ETC : WorkType.valueOf(value);
    }

    private void validateWorkItemCreateRules(ClientEntity client, WorkType workType, LocalDate dueDate) {
        if (client.getStatus() != ClientStatus.ACTIVE) {
            throw new IllegalArgumentException("CLIENT_INACTIVE");
        }
        if (client.getType() == ClientType.INDIVIDUAL && workType == WorkType.REVIEW) {
            throw new IllegalArgumentException("INVALID_WORK_TYPE_FOR_CLIENT_TYPE");
        }
        if (client.getTier() == ClientTier.VIP) {
            LocalDate maxDate = LocalDate.now().plusDays(14);
            if (dueDate != null && dueDate.isAfter(maxDate)) {
                throw new IllegalArgumentException("VIP_DUE_DATE_EXCEEDED");
            }
        }
    }

    private void validateWorkItemPatchRules(ClientEntity client, WorkStatus requestedStatus, WorkType requestedType) {
        if (requestedStatus == null) {
            if (requestedType == null) {
                return;
            }
        }
        if (client.getStatus() == ClientStatus.INACTIVE && requestedStatus != null && requestedStatus != WorkStatus.HOLD) {
            throw new IllegalArgumentException("CLIENT_INACTIVE_ONLY_HOLD_ALLOWED");
        }
        if (requestedType == WorkType.REVIEW && client.getType() == ClientType.INDIVIDUAL) {
            throw new IllegalArgumentException("INVALID_WORK_TYPE_FOR_CLIENT_TYPE");
        }
    }

    private SortSpec parseSort(List<String> sorts) {
        if (sorts == null || sorts.isEmpty() || sorts.get(0) == null || sorts.get(0).isBlank()) {
            return new SortSpec("id", "desc");
        }

        String first = sorts.get(0);
        String[] tokens = first.split(",");
        String field = tokens[0].trim();
        String direction = tokens.length > 1 ? tokens[1].trim().toLowerCase() : "desc";

        // Map frontend field names to database/mapper field names if necessary
        Map<String, String> fieldMapping = Map.of(
                "id", "id",
                "clientName", "clientName",
                "status", "status",
                "assignee", "assignee",
                "dueDate", "dueDate",
                "updatedAt", "updatedAt"
        );

        String mappedField = fieldMapping.get(field);
        if (mappedField == null) {
            mappedField = "id";
        }

        if (!"asc".equals(direction) && !"desc".equals(direction)) {
            direction = "desc";
        }
        return new SortSpec(mappedField, direction);
    }

    private String toJson(List<String> tags) {
        try {
            return objectMapper.writeValueAsString(tags == null ? Collections.emptyList() : tags);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> parseTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(tagsJson, new TypeReference<>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private int effectiveBulkChunkSize() {
        int configured = appSettings.getPerformance().getBulkChunkSize();
        return Math.min(10000, Math.max(200, configured));
    }

    private void assertNotCancelled(String jobId) {
        if (jobService.isCancellationRequested(jobId)) {
            throw new JobCancelledException("JOB_CANCELLED");
        }
    }

    private String toBulkPayload(BulkInsertRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (Exception ex) {
            return null;
        }
    }

    private record SortSpec(String field, String direction) {
    }

    private record CsvHeaderInfo(boolean hasRequestIdColumn) {
    }

    private record CsvParsedItem(BulkWorkItemInput item, int lineNo, String rawLine) {
    }

    private record CsvChunkResult(int successRows, int failureRows, Map<String, Integer> reasonCounts) {
    }

    private record CsvProcessResult(int totalRows, int successRows, int failureRows, Map<String, Integer> reasonCounts) {
    }

    @PreDestroy
    void shutdownBulkExecutor() {
        bulkExecutor.shutdown();
    }
}
