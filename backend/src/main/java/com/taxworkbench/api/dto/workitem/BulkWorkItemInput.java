package com.taxworkbench.api.dto.workitem;

import com.taxworkbench.api.model.WorkType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record BulkWorkItemInput(
        @NotNull Long clientId,
        @NotNull WorkType type,
        @NotBlank String assignee,
        @NotNull LocalDate dueDate,
        List<String> tags,
        String memo
) {
}
