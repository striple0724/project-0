package com.taxworkbench.api.dto.workitem;

import com.taxworkbench.api.model.WorkStatus;
import com.taxworkbench.api.model.WorkType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record CreateWorkItemRequest(
        @NotNull Long clientId,
        @NotNull WorkType type,
        WorkStatus status,
        @NotBlank @Size(max = 100) String assignee,
        @NotNull LocalDate dueDate,
        List<@NotBlank String> tags,
        @Size(max = 2000) String memo
) {
}
