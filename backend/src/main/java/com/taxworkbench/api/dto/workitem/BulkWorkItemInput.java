package com.taxworkbench.api.dto.workitem;

import com.taxworkbench.api.model.WorkType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "대량 등록 업무 입력 항목")
public record BulkWorkItemInput(
        @Schema(description = "고객사 ID", example = "101")
        @NotNull Long clientId,
        @Schema(description = "업무 유형", implementation = WorkType.class, example = "FILING")
        @NotNull WorkType type,
        @Schema(description = "담당자", example = "kim")
        @NotBlank String assignee,
        @Schema(description = "마감일", example = "2025-12-31")
        @NotNull LocalDate dueDate,
        @Schema(description = "태그 목록")
        List<String> tags,
        @Schema(description = "메모", example = "일괄등록")
        String memo
) {
}
