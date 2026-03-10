package com.taxworkbench.api.infrastructure.jpa.repository;

import com.taxworkbench.api.infrastructure.jpa.entity.WorkItemEntity;
import com.taxworkbench.api.model.WorkStatus;
import com.taxworkbench.api.model.WorkType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface WorkItemJpaRepository extends JpaRepository<WorkItemEntity, Long> {
    long countByStatus(WorkStatus status);

    boolean existsByClientId(Long clientId);

    boolean existsByClientIdAndType(Long clientId, WorkType type);

    List<WorkItemEntity> findByClientIdAndStatusIn(Long clientId, Collection<WorkStatus> statuses);

    @EntityGraph(attributePaths = {"client"})
    List<WorkItemEntity> findByIdIn(Collection<Long> ids);
}
