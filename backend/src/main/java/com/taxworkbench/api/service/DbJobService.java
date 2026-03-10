package com.taxworkbench.api.service;

import com.taxworkbench.api.dto.common.JobAcceptedResponse;
import com.taxworkbench.api.dto.export.ExportJobStatusResponse;
import com.taxworkbench.api.exception.ResourceNotFoundException;
import com.taxworkbench.api.infrastructure.jpa.entity.JobEntity;
import com.taxworkbench.api.infrastructure.jpa.repository.JobJpaRepository;
import com.taxworkbench.api.model.JobStatus;
import com.taxworkbench.api.model.JobType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@Transactional
public class DbJobService implements JobService {
    private final JobJpaRepository jobRepository;

    public DbJobService(JobJpaRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Override
    public JobAcceptedResponse createJob(String requestId, JobType jobType) {
        return createJob(requestId, jobType, null);
    }

    @Override
    public JobAcceptedResponse createJob(String requestId, JobType jobType, String payloadJson) {
        JobEntity entity = new JobEntity();
        String jobId = "job-" + UUID.randomUUID();
        entity.setId(jobId);
        entity.setRequestId(requestId);
        entity.setJobType(jobType);
        entity.setStatus(JobStatus.QUEUED);
        entity.setPayloadJson(payloadJson);
        jobRepository.save(entity);
        return new JobAcceptedResponse(requestId, jobId, jobType, JobStatus.QUEUED);
    }

    @Override
    public void markRunning(String jobId) {
        JobEntity entity = findJob(jobId);
        entity.setStatus(JobStatus.RUNNING);
        jobRepository.save(entity);
    }

    @Override
    public void markDone(String jobId, String downloadUrl, String filePath, OffsetDateTime expiresAt) {
        JobEntity entity = findJob(jobId);
        entity.setStatus(JobStatus.DONE);
        entity.setDownloadUrl(downloadUrl);
        entity.setFilePath(filePath);
        entity.setErrorMessage(null);
        entity.setExpiresAt(expiresAt);
        jobRepository.save(entity);
    }

    @Override
    public void markFailed(String jobId, String errorMessage) {
        JobEntity entity = findJob(jobId);
        entity.setStatus(JobStatus.FAILED);
        entity.setErrorMessage(errorMessage);
        jobRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public String getPayload(String jobId) {
        return findJob(jobId).getPayloadJson();
    }

    @Override
    @Transactional(readOnly = true)
    public String getFilePath(String jobId) {
        return findJob(jobId).getFilePath();
    }

    @Override
    @Transactional(readOnly = true)
    public ExportJobStatusResponse getExportStatus(String jobId) {
        JobEntity entity = findJob(jobId);
        if (entity.getJobType() != JobType.EXPORT) {
            throw new ResourceNotFoundException("Export job not found: " + jobId);
        }
        return new ExportJobStatusResponse(
                entity.getRequestId(),
                entity.getId(),
                entity.getJobType(),
                entity.getStatus(),
                entity.getErrorMessage(),
                entity.getDownloadUrl(),
                entity.getExpiresAt()
        );
    }

    private JobEntity findJob(String jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));
    }
}
