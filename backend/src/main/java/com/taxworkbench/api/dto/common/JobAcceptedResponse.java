package com.taxworkbench.api.dto.common;

import com.taxworkbench.api.model.JobStatus;
import com.taxworkbench.api.model.JobType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "비동기 작업 접수 응답")
public record JobAcceptedResponse(
        @Schema(description = "멱등/추적용 요청 ID", example = "req-20260310-001")
        String requestId,
        @Schema(description = "생성된 작업 ID", example = "job_abc123")
        String jobId,
        @Schema(description = "작업 유형", implementation = JobType.class, example = "EXPORT")
        JobType jobType,
        @Schema(description = "작업 상태", implementation = JobStatus.class, example = "QUEUED")
        JobStatus status
) {
}
