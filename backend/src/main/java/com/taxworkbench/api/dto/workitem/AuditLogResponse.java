package com.taxworkbench.api.dto.workitem;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "업무 이력 응답")
public record AuditLogResponse(
        @Schema(description = "이력 ID", example = "555")
        Long auditId,
        @Schema(description = "업무 ID", example = "10001")
        Long workItemId,
        @Schema(description = "변경 필드", example = "status")
        String field,
        @Schema(description = "변경 전 값", example = "TODO")
        String before,
        @Schema(description = "변경 후 값", example = "IN_PROGRESS")
        String after,
        @Schema(description = "변경자", example = "admin")
        String changedBy,
        @Schema(description = "변경 시각")
        OffsetDateTime changedAt
) {
}
