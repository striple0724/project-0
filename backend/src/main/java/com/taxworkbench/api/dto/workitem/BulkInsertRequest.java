package com.taxworkbench.api.dto.workitem;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "업무 대량 등록 요청")
public record BulkInsertRequest(
        @Schema(description = "요청 ID(멱등/추적용)", example = "bulk-20260310-001")
        @NotBlank String requestId,
        @Schema(description = "대량 등록 항목 목록")
        @NotEmpty List<@Valid BulkWorkItemInput> items
) {
}
