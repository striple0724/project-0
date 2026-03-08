package com.taxworkbench.api.service;

import com.taxworkbench.api.config.AppSettings;
import com.taxworkbench.api.dto.common.JobAcceptedResponse;
import com.taxworkbench.api.dto.export.ExportJobStatusResponse;
import com.taxworkbench.api.dto.workitem.WorkItemFilter;
import com.taxworkbench.api.model.JobType;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.nio.charset.StandardCharsets;

public class StubExportService implements ExportService {
    private final JobService jobService;
    private final AppSettings appSettings;

    public StubExportService(JobService jobService, AppSettings appSettings) {
        this.jobService = jobService;
        this.appSettings = appSettings;
    }

    @Override
    public JobAcceptedResponse submitExportJob(WorkItemFilter filter, String requestId) {
        return jobService.createJob(requestId, JobType.EXPORT);
    }

    @Override
    public StreamingResponseBody exportCsv(WorkItemFilter filter) {
        return outputStream -> {
            String header = "id,clientName,status,assignee,dueDate\n";
            String row = "101,ACME Tax Co.,TODO,kim,2026-03-25\n";
            byte[] bytes = (header + row).getBytes(StandardCharsets.UTF_8);
            long maxBytes = appSettings.getLimits().getDownload().getMaxBytes();
            if (bytes.length > maxBytes) {
                throw new IllegalStateException("DOWNLOAD_LIMIT_EXCEEDED");
            }
            outputStream.write(bytes);
            outputStream.flush();
        };
    }

    @Override
    public ExportJobStatusResponse getStatus(String jobId) {
        return jobService.getExportStatus(jobId);
    }
}
