package com.taxworkbench.api.dto.workitem;

import com.taxworkbench.api.model.WorkStatus;
import com.taxworkbench.api.model.WorkType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "업무 응답")
public record WorkItemResponse(
        @Schema(description = "업무 ID", example = "10001")
        Long id,
        @Schema(description = "고객사 ID", example = "101")
        Long clientId,
        @Schema(description = "고객사명", example = "ABC 세무법인")
        String clientName,
        @Schema(description = "사업자등록번호", example = "123-45-67890")
        String bizNo,
        @Schema(description = "업무 유형", implementation = WorkType.class, example = "FILING")
        WorkType type,
        @Schema(description = "업무 상태", implementation = WorkStatus.class, example = "TODO")
        WorkStatus status,
        @Schema(description = "담당자", example = "kim")
        String assignee,
        @Schema(description = "마감일", example = "2025-12-31")
        LocalDate dueDate,
        @Schema(description = "태그 목록")
        List<String> tags,
        @Schema(description = "메모", example = "메모 내용")
        String memo,
        @Schema(description = "낙관적 락 버전", example = "3")
        Long version,
        @Schema(description = "최종 수정 시각")
        OffsetDateTime updatedAt,
        @Schema(description = "이력 존재 여부", example = "true")
        Boolean hasAudit
) {
}
