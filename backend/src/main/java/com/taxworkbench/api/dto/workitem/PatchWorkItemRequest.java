package com.taxworkbench.api.dto.workitem;

import com.taxworkbench.api.model.WorkStatus;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record PatchWorkItemRequest(
        WorkStatus status,
        LocalDate dueDate,
        @Size(max = 100) String assignee,
        List<String> tags,
        @Size(max = 2000) String memo
) {
}
