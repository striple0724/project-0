package com.taxworkbench.api.controller;

import com.taxworkbench.api.dto.common.ApiResponse;
import com.taxworkbench.api.dto.common.JobAcceptedResponse;
import com.taxworkbench.api.dto.common.PagedResponse;
import com.taxworkbench.api.dto.workitem.*;
import com.taxworkbench.api.observability.RequestIdFilter;
import com.taxworkbench.api.service.WorkItemService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/work-items")
@Validated
public class WorkItemController {
    private final WorkItemService workItemService;

    public WorkItemController(WorkItemService workItemService) {
        this.workItemService = workItemService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<WorkItemResponse>> list(
            @RequestParam(required = false) String clientName,
            @RequestParam(required = false) com.taxworkbench.api.model.WorkStatus status,
            @RequestParam(required = false) String assignee,
            @RequestParam(required = false) String dueDateFrom,
            @RequestParam(required = false) String dueDateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) List<String> sort
    ) {
        WorkItemFilter filter = new WorkItemFilter(clientName, status, assignee, dueDateFrom, dueDateTo);
        return ResponseEntity.ok(workItemService.findAll(filter, page, size, sort));
    }

    @GetMapping("/{workItemId}")
    public ResponseEntity<ApiResponse<WorkItemResponse>> get(@PathVariable long workItemId) {
        return ResponseEntity.ok(new ApiResponse<>(workItemService.findById(workItemId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WorkItemResponse>> create(@Valid @RequestBody CreateWorkItemRequest request) {
        WorkItemResponse created = workItemService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/work-items/" + created.id()))
                .body(new ApiResponse<>(created));
    }

    @PatchMapping("/{workItemId}")
    public ResponseEntity<ApiResponse<WorkItemResponse>> patch(
            @PathVariable long workItemId,
            @RequestHeader("If-Match") String ifMatch,
            @Valid @RequestBody PatchWorkItemRequest request
    ) {
        long version = parseIfMatchVersion(ifMatch);
        WorkItemResponse updated = workItemService.patch(workItemId, version, request);
        return ResponseEntity.ok().eTag("\"" + updated.version() + "\"")
                .body(new ApiResponse<>(updated));
    }

    @DeleteMapping("/{workItemId}")
    public ResponseEntity<Void> delete(@PathVariable long workItemId) {
        workItemService.delete(workItemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{workItemId}/audit-logs")
    public ResponseEntity<PagedResponse<AuditLogResponse>> auditLogs(
            @PathVariable long workItemId,
            @RequestParam(required = false) String field,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(workItemService.findAuditLogs(workItemId, field, from, to, page, size));
    }

    @PostMapping(":bulk")
    public ResponseEntity<ApiResponse<JobAcceptedResponse>> bulk(
            @Valid @RequestBody BulkInsertRequest request,
            @RequestHeader(value = RequestIdFilter.REQUEST_ID_HEADER, required = false) String requestIdHeader
    ) {
        String requestId = requestIdHeader == null || requestIdHeader.isBlank() ? request.requestId() : requestIdHeader;
        return ResponseEntity.accepted().body(new ApiResponse<>(workItemService.submitBulk(request, requestId)));
    }

    private long parseIfMatchVersion(String ifMatch) {
        String cleaned = ifMatch.replace("\"", "").trim();
        return Long.parseLong(cleaned);
    }
}
