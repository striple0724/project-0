package com.taxworkbench.api.dto.workitem;

import com.taxworkbench.api.model.JobStatus;

public record BulkInsertAcceptedResponse(
        String jobId,
        int acceptedCount,
        JobStatus status
) {
}
