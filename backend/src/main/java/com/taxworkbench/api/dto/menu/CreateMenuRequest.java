package com.taxworkbench.api.dto.menu;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMenuRequest(
        @NotBlank @Size(max = 100) String menuId,
        Long parentPk,
        @NotNull @Min(0) @Max(10) Integer depthLevel,
        @NotBlank @Size(max = 30) String menuType,
        @Size(max = 255) String routePath,
        @NotNull @Min(0) Integer sortOrder,
        @NotBlank @Size(max = 1) String isVisible,
        @NotBlank @Size(max = 1) String isEnabled,
        @Size(max = 100) String icon,
        @NotBlank @Size(max = 20) String createdBy
) {
}
