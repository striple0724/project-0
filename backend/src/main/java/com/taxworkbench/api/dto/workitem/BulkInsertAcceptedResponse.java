package com.taxworkbench.api.dto.workitem;

import com.taxworkbench.api.model.JobStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "업무 대량 등록 접수 응답")
public record BulkInsertAcceptedResponse(
        @Schema(description = "작업 ID", example = "job_bulk_001")
        String jobId,
        @Schema(description = "접수 건수", example = "100")
        int acceptedCount,
        @Schema(description = "작업 상태", implementation = JobStatus.class, example = "QUEUED")
        JobStatus status
) {
}
