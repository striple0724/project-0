package com.taxworkbench.api.controller;

import com.taxworkbench.api.dto.common.ApiResponse;
import com.taxworkbench.api.dto.common.PagedResponse;
import com.taxworkbench.api.dto.workbench.JobMonitorItemResponse;
import com.taxworkbench.api.model.JobStatus;
import com.taxworkbench.api.model.JobType;
import com.taxworkbench.api.service.WorkbenchTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/jobs")
@Tag(name = "Admin Jobs", description = "관리자 Job 모니터링 API")
public class AdminJobController {
    private final WorkbenchTrackingService workbenchTrackingService;

    public AdminJobController(WorkbenchTrackingService workbenchTrackingService) {
        this.workbenchTrackingService = workbenchTrackingService;
    }

    @GetMapping
    @Operation(summary = "Job 모니터링 목록 조회", description = "요청ID/작업유형/상태 필터로 Job 처리 현황을 페이징 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = PagedResponse.class)))
    })
    public ResponseEntity<PagedResponse<JobMonitorItemResponse>> list(
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) JobType jobType,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(workbenchTrackingService.getJobMonitorItems(requestId, jobType, status, page, size));
    }

    @PostMapping("/{jobId}/cancel")
    @Operation(summary = "Job 정지 요청", description = "QUEUED/RUNNING 작업에 대해 정지 요청을 등록합니다.")
    public ResponseEntity<ApiResponse<JobMonitorItemResponse>> cancel(@PathVariable String jobId) {
        return ResponseEntity.ok(new ApiResponse<>(workbenchTrackingService.cancelJob(jobId)));
    }
}
