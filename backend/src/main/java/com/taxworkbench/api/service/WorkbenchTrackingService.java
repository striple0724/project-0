package com.taxworkbench.api.service;

import com.taxworkbench.api.dto.workbench.JobTrackingItemResponse;
import com.taxworkbench.api.dto.workbench.JobMonitorItemResponse;
import com.taxworkbench.api.dto.workbench.WorkbenchTrackingResponse;
import com.taxworkbench.api.infrastructure.jpa.entity.JobEntity;
import com.taxworkbench.api.infrastructure.jpa.repository.JobJpaRepository;
import com.taxworkbench.api.dto.common.PageInfo;
import com.taxworkbench.api.dto.common.PagedResponse;
import com.taxworkbench.api.infrastructure.jpa.repository.WorkItemJpaRepository;
import com.taxworkbench.api.model.JobStatus;
import com.taxworkbench.api.model.JobType;
import com.taxworkbench.api.model.WorkStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class WorkbenchTrackingService {
    private static final Set<JobStatus> ACTIVE_JOB_STATUSES = Set.of(JobStatus.QUEUED, JobStatus.RUNNING, JobStatus.CANCEL_REQUESTED);

    private final WorkItemJpaRepository workItemRepository;
    private final JobJpaRepository jobRepository;
    private final JobService jobService;

    public WorkbenchTrackingService(WorkItemJpaRepository workItemRepository, JobJpaRepository jobRepository, JobService jobService) {
        this.workItemRepository = workItemRepository;
        this.jobRepository = jobRepository;
        this.jobService = jobService;
    }

    public WorkbenchTrackingResponse getTrackingStatus() {
        long pendingApprovalCount = workItemRepository.countByStatus(WorkStatus.HOLD);
        long asyncJobCount = jobRepository.countByStatusIn(ACTIVE_JOB_STATUSES);
        long bulkAsyncCount = jobRepository.countByJobTypeAndStatusIn(JobType.BULK_INSERT, ACTIVE_JOB_STATUSES);
        long exportAsyncCount = jobRepository.countByJobTypeAndStatusIn(JobType.EXPORT, ACTIVE_JOB_STATUSES);

        List<JobTrackingItemResponse> recentJobs = jobRepository.findTop20ByOrderByCreatedAtDesc().stream()
                .map(this::toTrackingItem)
                .toList();

        return new WorkbenchTrackingResponse(
                pendingApprovalCount,
                asyncJobCount,
                bulkAsyncCount,
                exportAsyncCount,
                recentJobs
        );
    }

    public PagedResponse<JobMonitorItemResponse> getJobMonitorItems(
            String requestId,
            JobType jobType,
            JobStatus status,
            int page,
            int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(200, Math.max(size, 1));

        Specification<JobEntity> spec = (root, query, cb) -> cb.conjunction();
        if (requestId != null && !requestId.isBlank()) {
            String keyword = "%" + requestId.trim() + "%";
            spec = spec.and((root, query, cb) -> cb.like(root.get("requestId"), keyword));
        }
        if (jobType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("jobType"), jobType));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        Pageable pageable = PageRequest.of(safePage, safeSize, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        var result = jobRepository.findAll(spec, pageable);
        var rows = result.getContent().stream().map(this::toMonitorItem).toList();
        return new PagedResponse<>(rows, new PageInfo(safePage, safeSize, result.getTotalElements(), result.getTotalPages()));
    }

    @Transactional(readOnly = false)
    public JobMonitorItemResponse cancelJob(String jobId) {
        jobService.requestCancel(jobId);
        JobEntity entity = jobRepository.findById(jobId)
                .orElseThrow(() -> new com.taxworkbench.api.exception.ResourceNotFoundException("Job not found: " + jobId));
        return toMonitorItem(entity);
    }

    private JobTrackingItemResponse toTrackingItem(JobEntity entity) {
        return new JobTrackingItemResponse(
                entity.getId(),
                entity.getRequestId(),
                entity.getJobType(),
                entity.getStatus(),
                progressFromStatus(entity.getStatus()),
                entity.getErrorMessage(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private int progressFromStatus(JobStatus status) {
        if (status == null) {
            return 0;
        }
        return switch (status) {
            case QUEUED -> 0;
            case RUNNING -> 50;
            case CANCEL_REQUESTED -> 75;
            case PARTIAL_SUCCESS, DONE, FAILED, CANCELLED -> 100;
        };
    }

    private JobMonitorItemResponse toMonitorItem(JobEntity entity) {
        return new JobMonitorItemResponse(
                entity.getId(),
                entity.getRequestId(),
                entity.getJobType(),
                entity.getStatus(),
                progressFromStatus(entity.getStatus()),
                entity.getDownloadUrl(),
                entity.getErrorMessage(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getExpiresAt()
        );
    }
}
