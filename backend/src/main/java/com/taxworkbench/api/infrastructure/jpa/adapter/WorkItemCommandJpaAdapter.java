package com.taxworkbench.api.infrastructure.jpa.adapter;

import com.taxworkbench.api.application.port.command.WorkItemCommandStore;
import com.taxworkbench.api.infrastructure.jpa.entity.WorkItemEntity;
import com.taxworkbench.api.infrastructure.jpa.repository.WorkItemJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class WorkItemCommandJpaAdapter implements WorkItemCommandStore {
    private final WorkItemJpaRepository repository;

    public WorkItemCommandJpaAdapter(WorkItemJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public WorkItemEntity save(WorkItemEntity entity) {
        return repository.save(entity);
    }

    @Override
    public Optional<WorkItemEntity> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
