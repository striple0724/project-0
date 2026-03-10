package com.taxworkbench.api.dto.export;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "선택 항목 내보내기 작업 생성 요청")
public record CreateSelectedExportJobRequest(
        @NotEmpty
        @Schema(description = "내보낼 WorkItem ID 목록", example = "[101,102,103]")
        List<Long> workItemIds
) {
}
