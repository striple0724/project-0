package com.taxworkbench.api.controller;

import com.taxworkbench.api.dto.common.ApiResponse;
import com.taxworkbench.api.dto.common.JobAcceptedResponse;
import com.taxworkbench.api.dto.export.CreateExportJobRequest;
import com.taxworkbench.api.dto.export.CreateSelectedExportJobRequest;
import com.taxworkbench.api.dto.export.ExportJobStatusResponse;
import com.taxworkbench.api.dto.workitem.WorkItemFilter;
import com.taxworkbench.api.observability.RequestIdFilter;
import com.taxworkbench.api.service.ExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
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
@Tag(name = "Export", description = "내보내기(Export) API")
public class ExportController {
    private final ExportService exportService;

    public ExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @PostMapping("/exports")
    @Operation(
            summary = "내보내기 작업 제출",
            description = "필터 조건으로 CSV 내보내기 비동기 작업을 제출합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CreateExportJobRequest.class),
                            examples = @ExampleObject(
                                    name = "ExportByDateRange",
                                    value = "{\"clientName\":\"ABC\",\"status\":\"IN_PROGRESS\",\"assignee\":\"kim\",\"dueDateFrom\":\"2025-01-01\",\"dueDateTo\":\"2025-12-31\"}"
                            )
                    )
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "작업 접수 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ApiResponse<JobAcceptedResponse>> submitExportJob(
            @RequestBody CreateExportJobRequest request,
            @Parameter(description = "요청 추적 헤더(X-Request-Id)", example = "export-20260310-001")
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

    @PostMapping("/exports/selected")
    @Operation(summary = "선택 항목 내보내기 작업 제출", description = "선택한 WorkItem ID 목록 기준 CSV 내보내기 비동기 작업을 제출합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "작업 접수 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ApiResponse<JobAcceptedResponse>> submitSelectedExportJob(
            @RequestBody CreateSelectedExportJobRequest request,
            @Parameter(description = "요청 추적 헤더(X-Request-Id)", example = "export-selected-20260310-001")
            @RequestHeader(value = RequestIdFilter.REQUEST_ID_HEADER, required = false) String requestIdHeader
    ) {
        String requestId = requestIdHeader == null || requestIdHeader.isBlank()
                ? "export-selected-" + System.currentTimeMillis()
                : requestIdHeader;
        JobAcceptedResponse accepted = exportService.submitSelectedExportJob(request.workItemIds(), requestId);
        return ResponseEntity.accepted().body(new ApiResponse<>(accepted));
    }

    @GetMapping("/work-items:export")
    @Operation(summary = "현재 조건 즉시 CSV 스트리밍", description = "필터된 업무 데이터를 서버 스트리밍으로 바로 다운로드합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "CSV 다운로드 성공",
                    content = @Content(mediaType = "text/csv")),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "413", description = "다운로드 용량 한도 초과",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<StreamingResponseBody> exportCsv(
            @Parameter(description = "업체명 검색") @RequestParam(required = false) String clientName,
            @Parameter(description = "업무 상태 필터") @RequestParam(required = false) com.taxworkbench.api.model.WorkStatus status,
            @Parameter(description = "담당자 검색") @RequestParam(required = false) String assignee,
            @Parameter(description = "마감일 조회 시작 (yyyy-MM-dd)") @RequestParam(required = false) String dueDateFrom,
            @Parameter(description = "마감일 조회 종료 (yyyy-MM-dd)") @RequestParam(required = false) String dueDateTo
    ) {
        WorkItemFilter filter = new WorkItemFilter(clientName, status, assignee, dueDateFrom, dueDateTo);
        StreamingResponseBody stream = exportService.exportCsv(filter);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"work-items.csv\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(stream);
    }

    @GetMapping("/exports/{jobId}/download")
    @Operation(summary = "작업 결과 다운로드", description = "완료된 export 작업 결과를 CSV로 다운로드합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "CSV 다운로드 성공",
                    content = @Content(mediaType = "text/csv")),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "작업 없음",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "413", description = "다운로드 용량 한도 초과",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<StreamingResponseBody> downloadByJob(@PathVariable String jobId) {
        StreamingResponseBody stream = exportService.downloadCsv(jobId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"work-items-" + jobId + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(stream);
    }

    @GetMapping("/exports/{jobId}")
    @Operation(summary = "내보내기 작업 상태 조회", description = "export 작업의 진행 상태를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "작업 없음",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ApiResponse<ExportJobStatusResponse>> exportStatus(@PathVariable String jobId) {
        return ResponseEntity.ok(new ApiResponse<>(exportService.getStatus(jobId)));
    }
}
