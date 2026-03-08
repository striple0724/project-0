package com.taxworkbench.api.service;

import com.taxworkbench.api.dto.common.JobAcceptedResponse;
import com.taxworkbench.api.dto.export.ExportJobStatusResponse;
import com.taxworkbench.api.model.JobType;

public interface JobService {
    JobAcceptedResponse createJob(String requestId, JobType jobType);

    ExportJobStatusResponse getExportStatus(String jobId);
}
