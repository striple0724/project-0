package com.taxworkbench.api.service;

import com.taxworkbench.api.application.port.query.WorkItemQueryStore;
import com.taxworkbench.api.config.AppSettings;
import com.taxworkbench.api.dto.common.JobAcceptedResponse;
import com.taxworkbench.api.dto.export.ExportJobStatusResponse;
import com.taxworkbench.api.dto.workitem.WorkItemFilter;
import com.taxworkbench.api.infrastructure.mybatis.model.WorkItemGridRow;
import com.taxworkbench.api.model.JobType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class HybridExportService implements ExportService {
    private final JobService jobService;
    private final WorkItemQueryStore queryStore;
    private final AppSettings appSettings;

    public HybridExportService(JobService jobService, WorkItemQueryStore queryStore, AppSettings appSettings) {
        this.jobService = jobService;
        this.queryStore = queryStore;
        this.appSettings = appSettings;
    }

    @Override
    public JobAcceptedResponse submitExportJob(WorkItemFilter filter, String requestId) {
        return jobService.createJob(requestId, JobType.EXPORT);
    }

    @Override
    public StreamingResponseBody exportCsv(WorkItemFilter filter) {
        return outputStream -> {
            long maxBytes = appSettings.getLimits().getDownload().getMaxBytes();
            long written = 0;

            String header = "id,clientName,status,assignee,dueDate,updatedAt\n";
            byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
            if (headerBytes.length > maxBytes) {
                throw new IllegalStateException("DOWNLOAD_LIMIT_EXCEEDED");
            }
            outputStream.write(headerBytes);
            written += headerBytes.length;

            int page = 0;
            int size = 1000;
            while (true) {
                List<WorkItemGridRow> rows = queryStore.search(
                        filter.clientName(),
                        filter.status() == null ? null : filter.status().name(),
                        filter.assignee(),
                        page * size,
                        size
                );
                if (rows.isEmpty()) {
                    break;
                }

                for (WorkItemGridRow row : rows) {
                    String line = String.format(
                            "%d,%s,%s,%s,%s,%s%n",
                            row.id(),
                            escapeCsv(row.clientName()),
                            escapeCsv(row.status()),
                            escapeCsv(row.assignee()),
                            escapeCsv(row.dueDate()),
                            escapeCsv(row.updatedAt())
                    );
                    byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
                    if (written + bytes.length > maxBytes) {
                        throw new IllegalStateException("DOWNLOAD_LIMIT_EXCEEDED");
                    }
                    outputStream.write(bytes);
                    written += bytes.length;
                }
                if (rows.size() < size) {
                    break;
                }
                page++;
            }
            outputStream.flush();
        };
    }

    @Override
    public ExportJobStatusResponse getStatus(String jobId) {
        return jobService.getExportStatus(jobId);
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
}
