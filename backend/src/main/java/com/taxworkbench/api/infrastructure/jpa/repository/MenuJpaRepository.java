package com.taxworkbench.api.infrastructure.jpa.repository;

import com.taxworkbench.api.infrastructure.jpa.entity.MenuEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuJpaRepository extends JpaRepository<MenuEntity, Long> {
    boolean existsByMenuId(String menuId);

    boolean existsByParentPk(Long parentPk);

    List<MenuEntity> findAllByOrderBySortOrderAscMenuPkAsc();
}
