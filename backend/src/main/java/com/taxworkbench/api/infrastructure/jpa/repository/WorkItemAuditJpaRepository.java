package com.taxworkbench.api.infrastructure.jpa.repository;

import com.taxworkbench.api.infrastructure.jpa.entity.WorkItemAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WorkItemAuditJpaRepository extends JpaRepository<WorkItemAuditEntity, Long> {
    Page<WorkItemAuditEntity> findByWorkItemIdOrderByChangedAtDesc(Long workItemId, Pageable pageable);

    boolean existsByWorkItemId(Long workItemId);
}
