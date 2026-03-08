package com.taxworkbench.api.application.port.command;

import com.taxworkbench.api.infrastructure.jpa.entity.WorkItemEntity;

import java.util.Optional;

public interface WorkItemCommandStore {
    WorkItemEntity save(WorkItemEntity entity);

    Optional<WorkItemEntity> findById(Long id);

    void deleteById(Long id);
}
