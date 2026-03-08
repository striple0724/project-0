package com.taxworkbench.api.controller;

import com.taxworkbench.api.dto.common.ApiResponse;
import com.taxworkbench.api.dto.common.JobAcceptedResponse;
import com.taxworkbench.api.dto.export.CreateExportJobRequest;
import com.taxworkbench.api.dto.export.ExportJobStatusResponse;
import com.taxworkbench.api.dto.workitem.WorkItemFilter;
import com.taxworkbench.api.observability.RequestIdFilter;
import com.taxworkbench.api.service.ExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/v1")
public class ExportController {
    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @PostMapping("/exports")
    public ResponseEntity<ApiResponse<JobAcceptedResponse>> submitExportJob(
            @RequestBody CreateExportJobRequest request,
            @RequestHeader(value = RequestIdFilter.REQUEST_ID_HEADER, required = false) String requestIdHeader
    ) {
        WorkItemFilter filter = new WorkItemFilter(
                request.clientName(),
                request.status(),
                request.assignee(),
                request.dueDateFrom(),
                request.dueDateTo()
        );
        String requestId = requestIdHeader == null || requestIdHeader.isBlank()
                ? "export-" + System.currentTimeMillis()
                : requestIdHeader;

        JobAcceptedResponse accepted = exportService.submitExportJob(filter, requestId);
        return ResponseEntity.accepted().body(new ApiResponse<>(accepted));
    }

    @GetMapping("/work-items:export")
    public ResponseEntity<StreamingResponseBody> exportCsv(
            @RequestParam(required = false) String clientName,
            @RequestParam(required = false) com.taxworkbench.api.model.WorkStatus status,
            @RequestParam(required = false) String assignee,
            @RequestParam(required = false) String dueDateFrom,
            @RequestParam(required = false) String dueDateTo
    ) {
        WorkItemFilter filter = new WorkItemFilter(clientName, status, assignee, dueDateFrom, dueDateTo);
        StreamingResponseBody stream = exportService.exportCsv(filter);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"work-items.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(stream);
    }

    @GetMapping("/exports/{jobId}")
    public ResponseEntity<ApiResponse<ExportJobStatusResponse>> exportStatus(@PathVariable String jobId) {
        return ResponseEntity.ok(new ApiResponse<>(exportService.getStatus(jobId)));
    }
}
