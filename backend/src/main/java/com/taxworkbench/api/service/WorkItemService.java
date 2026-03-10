package com.taxworkbench.api.service;

import com.taxworkbench.api.dto.common.PageInfo;
import com.taxworkbench.api.dto.common.JobAcceptedResponse;
import com.taxworkbench.api.dto.common.PagedResponse;
import com.taxworkbench.api.dto.workitem.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

public interface WorkItemService {
    PagedResponse<WorkItemResponse> findAll(WorkItemFilter filter, int page, int size, List<String> sort, boolean includeTotal);

    WorkItemResponse findById(long id);

    WorkItemResponse create(CreateWorkItemRequest request);

    WorkItemResponse patch(long id, long version, PatchWorkItemRequest request);

    void delete(long id);

    PagedResponse<AuditLogResponse> findAuditLogs(long id, String field, String from, String to, int page, int size);

    JobAcceptedResponse submitBulk(BulkInsertRequest request, String requestId);

    JobAcceptedResponse submitBulkCsv(MultipartFile file, String requestId);

    StreamingResponseBody downloadBulkFailureReport(String jobId);
}
