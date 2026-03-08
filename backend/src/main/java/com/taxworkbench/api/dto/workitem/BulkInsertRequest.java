package com.taxworkbench.api.dto.workitem;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BulkInsertRequest(
        @NotBlank String requestId,
        @NotEmpty List<@Valid BulkWorkItemInput> items
) {
}
