package com.taxworkbench.api.dto.common;

import com.taxworkbench.api.model.JobStatus;
import com.taxworkbench.api.model.JobType;

public record JobAcceptedResponse(
        String requestId,
        String jobId,
        JobType jobType,
        JobStatus status
) {
}
