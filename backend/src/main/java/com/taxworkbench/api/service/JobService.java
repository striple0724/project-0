package com.taxworkbench.api.service;

import com.taxworkbench.api.dto.common.JobAcceptedResponse;
import com.taxworkbench.api.dto.export.ExportJobStatusResponse;
import com.taxworkbench.api.model.JobType;

import java.time.OffsetDateTime;

public interface JobService {
    JobAcceptedResponse createJob(String requestId, JobType jobType);

    JobAcceptedResponse createJob(String requestId, JobType jobType, String payloadJson);

    void markRunning(String jobId);

    void markDone(String jobId, String downloadUrl, String filePath, OffsetDateTime expiresAt);

    void markFailed(String jobId, String errorMessage);

    String getPayload(String jobId);

    String getFilePath(String jobId);

    ExportJobStatusResponse getExportStatus(String jobId);
}
