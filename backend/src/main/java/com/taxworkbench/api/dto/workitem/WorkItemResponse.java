package com.taxworkbench.api.dto.workitem;

import com.taxworkbench.api.model.WorkStatus;
import com.taxworkbench.api.model.WorkType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record WorkItemResponse(
        Long id,
        Long clientId,
        String clientName,
        String bizNo,
        WorkType type,
        WorkStatus status,
        String assignee,
        LocalDate dueDate,
        List<String> tags,
        String memo,
        Long version,
        OffsetDateTime updatedAt
) {
}
