package com.taxworkbench.api.dto.menu;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "메뉴 수정 요청")
public record UpdateMenuRequest(
        @Schema(description = "부모 메뉴 PK", example = "1")
        Long parentPk,
        @Schema(description = "메뉴 깊이", example = "1")
        @NotNull @Min(0) @Max(10) Integer depthLevel,
        @Schema(description = "메뉴 유형", example = "MENU")
        @NotBlank @Size(max = 30) String menuType,
        @Schema(description = "라우트 경로", example = "/workbench")
        @Size(max = 255) String routePath,
        @Schema(description = "정렬 순서", example = "10")
        @NotNull @Min(0) Integer sortOrder,
        @Schema(description = "표시 여부(Y/N)", example = "Y")
        @NotBlank @Size(max = 1) String isVisible,
        @Schema(description = "활성화 여부(Y/N)", example = "Y")
        @NotBlank @Size(max = 1) String isEnabled,
        @Schema(description = "아이콘명", example = "briefcase")
        @Size(max = 100) String icon,
        @Schema(description = "수정자", example = "admin")
        @NotBlank @Size(max = 20) String updatedBy
) {
}
