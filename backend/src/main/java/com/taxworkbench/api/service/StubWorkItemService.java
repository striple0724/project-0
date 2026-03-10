package com.taxworkbench.api.service;

import com.taxworkbench.api.dto.common.PageInfo;
import com.taxworkbench.api.dto.common.JobAcceptedResponse;
import com.taxworkbench.api.dto.common.PagedResponse;
import com.taxworkbench.api.dto.workitem.*;
import com.taxworkbench.api.exception.ResourceNotFoundException;
import com.taxworkbench.api.model.JobType;
import com.taxworkbench.api.model.WorkStatus;
import com.taxworkbench.api.model.WorkType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public class StubWorkItemService implements WorkItemService {
    private final JobService jobService;

    public StubWorkItemService(JobService jobService) {
        this.jobService = jobService;
    }

    @Override
    public PagedResponse<WorkItemResponse> findAll(WorkItemFilter filter, int page, int size, List<String> sort, boolean includeTotal) {
        WorkItemResponse sample = sampleWorkItem(101L, 1L);
        return new PagedResponse<>(List.of(sample), new PageInfo(page, size, 1, 1));
    }

    @Override
    public WorkItemResponse findById(long id) {
        if (id <= 0) {
            throw new ResourceNotFoundException("WorkItem not found: " + id);
        }
        return sampleWorkItem(id, 2L);
    }

    @Override
    public WorkItemResponse create(CreateWorkItemRequest request) {
        return new WorkItemResponse(
                102L,
                request.clientId(),
                "Sample Client",
                "123-45-67890",
                request.type(),
                request.status() == null ? WorkStatus.TODO : request.status(),
                request.assignee(),
                request.dueDate(),
                request.tags(),
                request.memo(),
                1L,
                OffsetDateTime.now(),
                false
        );
    }

    @Override
    public WorkItemResponse patch(long id, long version, PatchWorkItemRequest request) {
        if (id <= 0) {
            throw new ResourceNotFoundException("WorkItem not found: " + id);
        }
        WorkStatus status = request.status() == null ? WorkStatus.IN_PROGRESS : request.status();
        LocalDate dueDate = request.dueDate() == null ? LocalDate.now().plusDays(3) : request.dueDate();

        return new WorkItemResponse(
                id,
                11L,
                "ACME Tax Co.",
                "123-45-67890",
                WorkType.FILING,
                status,
                request.assignee() == null ? "kim" : request.assignee(),
                dueDate,
                request.tags(),
                request.memo(),
                version + 1,
                OffsetDateTime.now(),
                true
        );
    }

    @Override
    public void delete(long id) {
        if (id <= 0) {
            throw new ResourceNotFoundException("WorkItem not found: " + id);
        }
    }

    @Override
    public PagedResponse<AuditLogResponse> findAuditLogs(long id, String field, String from, String to, int page, int size) {
        if (id <= 0) {
            throw new ResourceNotFoundException("WorkItem not found: " + id);
        }
        AuditLogResponse audit = new AuditLogResponse(
                90001L,
                id,
                field == null ? "status" : field,
                "TODO",
                "IN_PROGRESS",
                "kim",
                OffsetDateTime.now()
        );
        return new PagedResponse<>(List.of(audit), new PageInfo(page, size, 1, 1));
    }

    @Override
    public JobAcceptedResponse submitBulk(BulkInsertRequest request, String requestId) {
        return jobService.createJob(requestId, JobType.BULK_INSERT);
    }

    @Override
    public JobAcceptedResponse submitBulkCsv(MultipartFile file, String requestId) {
        return jobService.createJob(requestId, JobType.BULK_INSERT);
    }

    @Override
    public BulkCsvValidationResponse validateBulkCsv(MultipartFile file) {
        return new BulkCsvValidationResponse(0, 0, 0, 0, 0, List.of(), 0);
    }

    @Override
    public StreamingResponseBody downloadBulkFailureReport(String jobId) {
        throw new ResourceNotFoundException("Bulk failure report not found: " + jobId);
    }

    private WorkItemResponse sampleWorkItem(long id, long version) {
        return new WorkItemResponse(
                id,
                11L,
                "ACME Tax Co.",
                "123-45-67890",
                WorkType.FILING,
                WorkStatus.TODO,
                "kim",
                LocalDate.now().plusDays(7),
                List.of("vip", "urgent"),
                "Sample data",
                version,
                OffsetDateTime.now(),
                true
        );
    }
}
