package com.taxworkbench.api.infrastructure.jpa.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TB_MENU", indexes = {
        @Index(name = "IDX_TB_MENU_PARENT_PK", columnList = "PARENT_PK"),
        @Index(name = "IDX_TB_MENU_SORT_ORDER", columnList = "SORT_ORDER")
})
public class MenuEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MENU_PK")
    private Long menuPk;

    @Column(name = "MENU_ID", nullable = false, unique = true, length = 100)
    private String menuId;

    @Column(name = "PARENT_PK")
    private Long parentPk;

    @Column(name = "DEPTH_LEVEL", nullable = false)
    private Integer depthLevel;

    @Column(name = "MENU_TYPE", nullable = false, length = 30)
    private String menuType;

    @Column(name = "ROUTE_PATH", length = 255)
    private String routePath;

    @Column(name = "SORT_ORDER", nullable = false)
    private Integer sortOrder;

    @Column(name = "IS_VISIBLE", nullable = false, length = 1)
    private String isVisible;

    @Column(name = "IS_ENABLED", nullable = false, length = 1)
    private String isEnabled;

    @Column(name = "ICON", length = 100)
    private String icon;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "CREATED_BY", nullable = false, length = 20)
    private String createdBy;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "UPDATED_BY", length = 20)
    private String updatedBy;

    public Long getMenuPk() { return menuPk; }
    public String getMenuId() { return menuId; }
    public void setMenuId(String menuId) { this.menuId = menuId; }
    public Long getParentPk() { return parentPk; }
    public void setParentPk(Long parentPk) { this.parentPk = parentPk; }
    public Integer getDepthLevel() { return depthLevel; }
    public void setDepthLevel(Integer depthLevel) { this.depthLevel = depthLevel; }
    public String getMenuType() { return menuType; }
    public void setMenuType(String menuType) { this.menuType = menuType; }
    public String getRoutePath() { return routePath; }
    public void setRoutePath(String routePath) { this.routePath = routePath; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getIsVisible() { return isVisible; }
    public void setIsVisible(String isVisible) { this.isVisible = isVisible; }
    public String getIsEnabled() { return isEnabled; }
    public void setIsEnabled(String isEnabled) { this.isEnabled = isEnabled; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
