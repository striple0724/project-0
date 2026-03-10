package com.taxworkbench.api.infrastructure.jpa.repository;

import com.taxworkbench.api.infrastructure.jpa.entity.JobEntity;
import com.taxworkbench.api.model.JobStatus;
import com.taxworkbench.api.model.JobType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

public interface JobJpaRepository extends JpaRepository<JobEntity, String>, JpaSpecificationExecutor<JobEntity> {
    long countByStatusIn(Collection<JobStatus> statuses);

    long countByJobTypeAndStatusIn(JobType jobType, Collection<JobStatus> statuses);

    List<JobEntity> findByStatusIn(Collection<JobStatus> statuses);

    List<JobEntity> findTop20ByOrderByCreatedAtDesc();
}
