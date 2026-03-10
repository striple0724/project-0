package com.taxworkbench.api.service;

import com.taxworkbench.api.dto.common.JobAcceptedResponse;
import com.taxworkbench.api.dto.export.ExportJobStatusResponse;
import com.taxworkbench.api.infrastructure.jpa.entity.JobEntity;
import com.taxworkbench.api.model.JobStatus;
import com.taxworkbench.api.model.JobType;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

public interface JobService {
    JobAcceptedResponse createJob(String requestId, JobType jobType);

    JobAcceptedResponse createJob(String requestId, JobType jobType, String payloadJson);

    void markRunning(String jobId);

    void markDone(String jobId, String downloadUrl, String filePath, OffsetDateTime expiresAt);

    void markPartialSuccess(String jobId, String downloadUrl, String filePath, OffsetDateTime expiresAt, String message);

    void markFailed(String jobId, String errorMessage);

    void markCancelled(String jobId, String reason);

    JobStatus requestCancel(String jobId);

    JobStatus getStatus(String jobId);

    boolean isCancellationRequested(String jobId);

    String getPayload(String jobId);

    String getFilePath(String jobId);

    ExportJobStatusResponse getExportStatus(String jobId);

    List<JobEntity> findByStatuses(Collection<JobStatus> statuses);
}
