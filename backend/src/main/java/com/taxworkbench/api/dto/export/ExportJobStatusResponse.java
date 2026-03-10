package com.taxworkbench.api.dto.export;

import com.taxworkbench.api.model.JobStatus;
import com.taxworkbench.api.model.JobType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "내보내기 작업 상태 응답")
public record ExportJobStatusResponse(
        @Schema(description = "멱등/추적용 요청 ID", example = "req-20260310-001")
        String requestId,
        @Schema(description = "작업 ID", example = "job_abc123")
        String jobId,
        @Schema(description = "작업 유형", implementation = JobType.class, example = "EXPORT")
        JobType jobType,
        @Schema(description = "작업 상태", implementation = JobStatus.class, example = "DONE")
        JobStatus status,
        @Schema(description = "실패 사유(실패 시)", example = "DOWNLOAD_LIMIT_EXCEEDED")
        String errorMessage,
        @Schema(description = "다운로드 URL", example = "/api/v1/exports/job_abc123/download")
        String downloadUrl,
        @Schema(description = "다운로드 만료 시각", example = "2026-03-10T18:00:00+09:00")
        OffsetDateTime expiresAt
) {
}
