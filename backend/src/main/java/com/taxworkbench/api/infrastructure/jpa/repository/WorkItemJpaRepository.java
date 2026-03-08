package com.taxworkbench.api.infrastructure.jpa.repository;

import com.taxworkbench.api.infrastructure.jpa.entity.WorkItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkItemJpaRepository extends JpaRepository<WorkItemEntity, Long> {
}
