package com.taxworkbench.api.service;

import com.taxworkbench.api.dto.menu.CreateMenuRequest;
import com.taxworkbench.api.dto.menu.MenuResponse;
import com.taxworkbench.api.dto.menu.UpdateMenuRequest;
import com.taxworkbench.api.exception.ResourceNotFoundException;
import com.taxworkbench.api.infrastructure.jpa.entity.MenuEntity;
import com.taxworkbench.api.infrastructure.jpa.repository.MenuJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class MenuService {
    private final MenuJpaRepository menuRepository;

    public MenuService(MenuJpaRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    @Transactional(readOnly = true)
    public List<MenuResponse> findAll() {
        return menuRepository.findAllByOrderBySortOrderAscMenuPkAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MenuResponse findByPk(long menuPk) {
        MenuEntity menu = menuRepository.findById(menuPk)
                .orElseThrow(() -> new ResourceNotFoundException("Menu not found: " + menuPk));
        return toResponse(menu);
    }

    public MenuResponse create(CreateMenuRequest request) {
        if (menuRepository.existsByMenuId(request.menuId())) {
            throw new IllegalArgumentException("MENU_ID_ALREADY_EXISTS");
        }
        validateParent(request.parentPk(), null);

        MenuEntity menu = new MenuEntity();
        menu.setMenuId(request.menuId());
        menu.setParentPk(request.parentPk());
        menu.setDepthLevel(request.depthLevel());
        menu.setMenuType(request.menuType());
        menu.setRoutePath(blankToNull(request.routePath()));
        menu.setSortOrder(request.sortOrder());
        menu.setIsVisible(normalizeYn(request.isVisible()));
        menu.setIsEnabled(normalizeYn(request.isEnabled()));
        menu.setIcon(blankToNull(request.icon()));
        menu.setCreatedAt(LocalDateTime.now());
        menu.setCreatedBy(request.createdBy());

        return toResponse(menuRepository.save(menu));
    }

    public MenuResponse update(long menuPk, UpdateMenuRequest request) {
        MenuEntity menu = menuRepository.findById(menuPk)
                .orElseThrow(() -> new ResourceNotFoundException("Menu not found: " + menuPk));

        validateParent(request.parentPk(), menuPk);

        menu.setParentPk(request.parentPk());
        menu.setDepthLevel(request.depthLevel());
        menu.setMenuType(request.menuType());
        menu.setRoutePath(blankToNull(request.routePath()));
        menu.setSortOrder(request.sortOrder());
        menu.setIsVisible(normalizeYn(request.isVisible()));
        menu.setIsEnabled(normalizeYn(request.isEnabled()));
        menu.setIcon(blankToNull(request.icon()));
        menu.setUpdatedAt(LocalDateTime.now());
        menu.setUpdatedBy(request.updatedBy());

        return toResponse(menuRepository.save(menu));
    }

    public void delete(long menuPk) {
        if (!menuRepository.existsById(menuPk)) {
            throw new ResourceNotFoundException("Menu not found: " + menuPk);
        }
        if (menuRepository.existsByParentPk(menuPk)) {
            throw new IllegalArgumentException("MENU_HAS_CHILDREN");
        }
        menuRepository.deleteById(menuPk);
    }

    private void validateParent(Long parentPk, Long selfPk) {
        if (parentPk == null) {
            return;
        }
        if (selfPk != null && selfPk.equals(parentPk)) {
            throw new IllegalArgumentException("INVALID_PARENT_MENU");
        }
        if (!menuRepository.existsById(parentPk)) {
            throw new IllegalArgumentException("PARENT_MENU_NOT_FOUND");
        }
    }

    private String normalizeYn(String value) {
        return "Y".equalsIgnoreCase(value) ? "Y" : "N";
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private MenuResponse toResponse(MenuEntity menu) {
        return new MenuResponse(
                menu.getMenuPk(),
                menu.getMenuId(),
                menu.getParentPk(),
                menu.getDepthLevel(),
                menu.getMenuType(),
                menu.getRoutePath(),
                menu.getSortOrder(),
                menu.getIsVisible(),
                menu.getIsEnabled(),
                menu.getIcon(),
                menu.getCreatedAt(),
                menu.getCreatedBy(),
                menu.getUpdatedAt(),
                menu.getUpdatedBy()
        );
    }
}
