package com.taxworkbench.api.dto.export;

import com.taxworkbench.api.model.WorkStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내보내기 작업 생성 요청")
public record CreateExportJobRequest(
        @Schema(description = "고객사명 필터", example = "ABC")
        String clientName,
        @Schema(description = "업무 상태 필터", implementation = WorkStatus.class, example = "IN_PROGRESS")
        WorkStatus status,
        @Schema(description = "담당자 필터", example = "kim")
        String assignee,
        @Schema(description = "마감일 시작(포함)", example = "2025-01-01")
        String dueDateFrom,
        @Schema(description = "마감일 종료(포함)", example = "2025-12-31")
        String dueDateTo
) {
}
