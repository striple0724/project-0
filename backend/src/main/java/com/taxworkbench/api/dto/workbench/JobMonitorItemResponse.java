package com.taxworkbench.api.dto.workbench;

import com.taxworkbench.api.model.JobStatus;
import com.taxworkbench.api.model.JobType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "관리자 Job 모니터링 항목")
public record JobMonitorItemResponse(
        @Schema(description = "작업 ID", example = "job-123")
        String jobId,
        @Schema(description = "요청 ID", example = "bulk-file-20260310-001")
        String requestId,
        @Schema(description = "작업 유형", implementation = JobType.class, example = "BULK_INSERT")
        JobType jobType,
        @Schema(description = "작업 상태", implementation = JobStatus.class, example = "RUNNING")
        JobStatus status,
        @Schema(description = "진행률(%)", example = "50")
        int progressPercent,
        @Schema(description = "다운로드 URL(Export 또는 실패리포트)", example = "/api/v1/work-items/bulk-jobs/job-123/failures")
        String downloadUrl,
        @Schema(description = "오류 메시지", example = "CSV_INVALID_HEADER")
        String errorMessage,
        @Schema(description = "생성 시각")
        OffsetDateTime createdAt,
        @Schema(description = "수정 시각")
        OffsetDateTime updatedAt,
        @Schema(description = "만료 시각")
        OffsetDateTime expiresAt
) {
}
