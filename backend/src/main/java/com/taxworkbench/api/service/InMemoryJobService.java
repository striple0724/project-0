package com.taxworkbench.api.service;

import com.taxworkbench.api.config.AppSettings;
import com.taxworkbench.api.dto.common.JobAcceptedResponse;
import com.taxworkbench.api.dto.export.ExportJobStatusResponse;
import com.taxworkbench.api.exception.ResourceNotFoundException;
import com.taxworkbench.api.model.JobStatus;
import com.taxworkbench.api.model.JobType;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryJobService implements JobService {
    private final Map<String, JobRecord> jobs = new ConcurrentHashMap<>();
    private final AppSettings appSettings;

    public InMemoryJobService(AppSettings appSettings) {
        this.appSettings = appSettings;
    }

    @Override
    public JobAcceptedResponse createJob(String requestId, JobType jobType) {
        String jobId = "job-" + UUID.randomUUID();
        JobRecord record = new JobRecord(requestId, jobId, jobType, JobStatus.QUEUED, OffsetDateTime.now());
        jobs.put(jobId, record);

        if (jobType == JobType.EXPORT) {
            jobs.put(jobId, record.withStatus(JobStatus.DONE));
        }
        return new JobAcceptedResponse(requestId, jobId, jobType, JobStatus.QUEUED);
    }

    @Override
    public ExportJobStatusResponse getExportStatus(String jobId) {
        JobRecord record = jobs.get(jobId);
        if (record == null) {
            throw new ResourceNotFoundException("Export job not found: " + jobId);
        }
        if (record.jobType() != JobType.EXPORT) {
            throw new ResourceNotFoundException("Export job not found: " + jobId);
        }
        String downloadUrl = record.status() == JobStatus.DONE
                ? appSettings.getRuntime().getPublicBaseUrl() + "/api/v1/work-items:export"
                : null;

        return new ExportJobStatusResponse(
                record.requestId(),
                record.jobId(),
                record.jobType(),
                record.status(),
                downloadUrl,
                OffsetDateTime.now().plusHours(1)
        );
    }

    private record JobRecord(
            String requestId,
            String jobId,
            JobType jobType,
            JobStatus status,
            OffsetDateTime createdAt
    ) {
        private JobRecord withStatus(JobStatus next) {
            return new JobRecord(requestId, jobId, jobType, next, createdAt);
        }
    }
}
