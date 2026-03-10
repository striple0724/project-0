package com.taxworkbench.api.dto.menu;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "메뉴 응답")
public record MenuResponse(
        @Schema(description = "메뉴 PK", example = "1")
        Long menuPk,
        @Schema(description = "메뉴 ID", example = "workbench")
        String menuId,
        @Schema(description = "부모 메뉴 PK", example = "0")
        Long parentPk,
        @Schema(description = "메뉴 깊이", example = "1")
        Integer depthLevel,
        @Schema(description = "메뉴 유형", example = "MENU")
        String menuType,
        @Schema(description = "라우트 경로", example = "/workbench")
        String routePath,
        @Schema(description = "정렬 순서", example = "10")
        Integer sortOrder,
        @Schema(description = "표시 여부(Y/N)", example = "Y")
        String isVisible,
        @Schema(description = "활성화 여부(Y/N)", example = "Y")
        String isEnabled,
        @Schema(description = "아이콘명", example = "briefcase")
        String icon,
        @Schema(description = "생성 일시")
        LocalDateTime createdAt,
        @Schema(description = "생성자", example = "admin")
        String createdBy,
        @Schema(description = "수정 일시")
        LocalDateTime updatedAt,
        @Schema(description = "수정자", example = "admin")
        String updatedBy
) {
}
