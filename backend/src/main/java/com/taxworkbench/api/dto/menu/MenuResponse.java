package com.taxworkbench.api.dto.menu;

import java.time.LocalDateTime;

public record MenuResponse(
        Long menuPk,
        String menuId,
        Long parentPk,
        Integer depthLevel,
        String menuType,
        String routePath,
        Integer sortOrder,
        String isVisible,
        String isEnabled,
        String icon,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime updatedAt,
        String updatedBy
) {
}
