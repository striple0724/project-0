package com.taxworkbench.api.dto.workitem;

import com.taxworkbench.api.model.WorkStatus;

public record WorkItemFilter(
        String clientName,
        WorkStatus status,
        String assignee,
        String dueDateFrom,
        String dueDateTo
) {
}
