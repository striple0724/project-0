package com.taxworkbench.api.dto.export;

import com.taxworkbench.api.model.WorkStatus;

public record CreateExportJobRequest(
        String clientName,
        WorkStatus status,
        String assignee,
        String dueDateFrom,
        String dueDateTo
) {
}
