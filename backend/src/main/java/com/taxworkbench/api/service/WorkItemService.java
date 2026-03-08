package com.taxworkbench.api.service;

import com.taxworkbench.api.dto.common.PageInfo;
import com.taxworkbench.api.dto.common.JobAcceptedResponse;
import com.taxworkbench.api.dto.common.PagedResponse;
import com.taxworkbench.api.dto.workitem.*;

import java.util.List;

public interface WorkItemService {
    PagedResponse<WorkItemResponse> findAll(WorkItemFilter filter, int page, int size, List<String> sort);

    WorkItemResponse findById(long id);

    WorkItemResponse create(CreateWorkItemRequest request);

    WorkItemResponse patch(long id, long version, PatchWorkItemRequest request);

    void delete(long id);

    PagedResponse<AuditLogResponse> findAuditLogs(long id, String field, String from, String to, int page, int size);

    JobAcceptedResponse submitBulk(BulkInsertRequest request, String requestId);
}
