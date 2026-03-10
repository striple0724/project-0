package com.taxworkbench.api.dto.workitem;

import com.taxworkbench.api.model.WorkStatus;
import com.taxworkbench.api.model.WorkType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "업무 생성 요청")
public record CreateWorkItemRequest(
        @Schema(description = "고객사 ID", example = "101")
        @NotNull Long clientId,
        @Schema(description = "업무 유형", implementation = WorkType.class, example = "FILING")
        @NotNull WorkType type,
        @Schema(description = "업무 상태", implementation = WorkStatus.class, example = "TODO")
        WorkStatus status,
        @Schema(description = "담당자", example = "kim")
        @NotBlank @Size(max = 100) String assignee,
        @Schema(description = "마감일", example = "2025-12-31")
        @NotNull LocalDate dueDate,
        @Schema(description = "태그 목록", example = "[\"VAT\", \"Q4\"]")
        List<@NotBlank String> tags,
        @Schema(description = "메모", example = "우선순위 높음")
        @Size(max = 2000) String memo
) {
}
