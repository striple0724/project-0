package com.taxworkbench.api.dto.workitem;

import java.time.OffsetDateTime;

public record AuditLogResponse(
        Long auditId,
        Long workItemId,
        String field,
        String before,
        String after,
        String changedBy,
        OffsetDateTime changedAt
) {
}
