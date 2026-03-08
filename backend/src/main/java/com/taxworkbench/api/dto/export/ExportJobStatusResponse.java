package com.taxworkbench.api.dto.export;

import com.taxworkbench.api.model.JobStatus;
import com.taxworkbench.api.model.JobType;

import java.time.OffsetDateTime;

public record ExportJobStatusResponse(
        String requestId,
        String jobId,
        JobType jobType,
        JobStatus status,
        String downloadUrl,
        OffsetDateTime expiresAt
) {
}
