package com.taxworkbench.api.service;

import com.taxworkbench.api.dto.common.JobAcceptedResponse;
import com.taxworkbench.api.dto.export.ExportJobStatusResponse;
import com.taxworkbench.api.dto.workitem.WorkItemFilter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

public interface ExportService {
    JobAcceptedResponse submitExportJob(WorkItemFilter filter, String requestId);

    StreamingResponseBody exportCsv(WorkItemFilter filter);

    ExportJobStatusResponse getStatus(String jobId);
}
