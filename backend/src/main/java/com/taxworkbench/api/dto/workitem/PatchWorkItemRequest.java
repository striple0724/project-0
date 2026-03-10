package com.taxworkbench.api.dto.workitem;

import com.taxworkbench.api.model.WorkStatus;
import com.taxworkbench.api.model.WorkType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "업무 부분 수정 요청")
public record PatchWorkItemRequest(
        @Schema(description = "업무 상태", implementation = WorkStatus.class, example = "IN_PROGRESS")
        WorkStatus status,
        @Schema(description = "업무 유형", implementation = WorkType.class, example = "REVIEW")
        WorkType type,
        @Schema(description = "마감일", example = "2025-12-31")
        LocalDate dueDate,
        @Schema(description = "담당자", example = "kim")
        @Size(max = 100) String assignee,
        @Schema(description = "태그 목록", example = "[\"VAT\", \"Q4\"]")
        List<String> tags,
        @Schema(description = "메모", example = "검토 우선")
        @Size(max = 2000) String memo
) {
}
