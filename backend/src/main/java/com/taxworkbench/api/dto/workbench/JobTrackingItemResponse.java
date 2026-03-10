package com.taxworkbench.api.dto.workbench;

import com.taxworkbench.api.model.JobStatus;
import com.taxworkbench.api.model.JobType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "작업 추적 항목")
public record JobTrackingItemResponse(
        @Schema(description = "작업 ID", example = "job_abc123")
        String jobId,
        @Schema(description = "요청 ID", example = "req-20260310-001")
        String requestId,
        @Schema(description = "작업 유형", implementation = JobType.class, example = "EXPORT")
        JobType jobType,
        @Schema(description = "작업 상태", implementation = JobStatus.class, example = "RUNNING")
        JobStatus status,
        @Schema(description = "진행률(%)", example = "65")
        int progressPercent,
        @Schema(description = "오류 메시지", example = "validation failed")
        String errorMessage,
        @Schema(description = "생성 시각")
        OffsetDateTime createdAt,
        @Schema(description = "수정 시각")
        OffsetDateTime updatedAt
) {
}
