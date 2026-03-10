package com.taxworkbench.api.service;

import com.taxworkbench.api.infrastructure.jpa.entity.JobEntity;
import com.taxworkbench.api.model.JobStatus;
import com.taxworkbench.api.model.JobType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class InterruptedJobRecoveryService implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(InterruptedJobRecoveryService.class);
    private static final Set<JobStatus> RECOVERABLE_STATUSES = Set.of(JobStatus.QUEUED, JobStatus.RUNNING, JobStatus.CANCEL_REQUESTED);
    private static final String RECOVERY_MESSAGE = "JOB_INTERRUPTED_BY_SERVER_RESTART";

    private final JobService jobService;
    private final ExportService exportService;

    public InterruptedJobRecoveryService(JobService jobService, ExportService exportService) {
        this.jobService = jobService;
        this.exportService = exportService;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<JobEntity> interruptedJobs = jobService.findByStatuses(RECOVERABLE_STATUSES);
        if (interruptedJobs.isEmpty()) {
            return;
        }

        int resumedExports = 0;
        int failedJobs = 0;
        int cancelledJobs = 0;
        for (JobEntity job : interruptedJobs) {
            JobStatus previousStatus = job.getStatus();
            if (previousStatus == JobStatus.CANCEL_REQUESTED) {
                jobService.markCancelled(job.getId(), RECOVERY_MESSAGE + ":" + previousStatus.name());
                cancelledJobs++;
                continue;
            }
            if (job.getJobType() == JobType.EXPORT && exportService.resumeJob(job.getId())) {
                resumedExports++;
                continue;
            }
            jobService.markFailed(job.getId(), RECOVERY_MESSAGE + ":" + previousStatus.name());
            failedJobs++;
        }
        log.warn("Recovered interrupted jobs after server restart: resumedExports={}, cancelledJobs={}, failedJobs={}", resumedExports, cancelledJobs, failedJobs);
    }
}
