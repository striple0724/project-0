package com.taxworkbench.api.controller;

import com.taxworkbench.api.dto.common.ApiResponse;
import com.taxworkbench.api.dto.common.JobAcceptedResponse;
import com.taxworkbench.api.dto.common.PagedResponse;
import com.taxworkbench.api.dto.workitem.*;
import com.taxworkbench.api.observability.RequestIdFilter;
import com.taxworkbench.api.service.WorkItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/work-items")
@Validated
@Tag(name = "WorkItems", description = "업무(WorkItem) 조회/생성/수정/삭제 API")
public class WorkItemController {
    private final WorkItemService workItemService;

    public WorkItemController(WorkItemService workItemService) {
        this.workItemService = workItemService;
    }

    @GetMapping
    @Operation(summary = "업무 목록 조회", description = "필터/정렬/페이징 조건으로 업무 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = PagedResponse.class)))
    })
    public ResponseEntity<PagedResponse<WorkItemResponse>> list(
            @Parameter(description = "업체명 검색 (부분 일치)", example = "ABC")
            @RequestParam(required = false) String clientName,
            @Parameter(description = "업무 상태 필터")
            @RequestParam(required = false) com.taxworkbench.api.model.WorkStatus status,
            @Parameter(description = "담당자명 검색")
            @RequestParam(required = false) String assignee,
            @Parameter(description = "마감일 조회 시작 (yyyy-MM-dd)", example = "2024-01-01")
            @RequestParam(required = false) String dueDateFrom,
            @Parameter(description = "마감일 조회 종료 (yyyy-MM-dd)", example = "2024-12-31")
            @RequestParam(required = false) String dueDateTo,
            @Parameter(description = "페이지 번호 (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 당 건수")
            @RequestParam(defaultValue = "50") int size,
            @Parameter(description = "전체 건수 포함 여부 (No-Offset 페이징 최적화 시 false 권장)")
            @RequestParam(defaultValue = "true") boolean includeTotal,
            @Parameter(description = "정렬 조건 (필드명,asc|desc). 지원 필드: id, clientName, status, assignee, dueDate, updatedAt", example = "dueDate,desc")
            @RequestParam(required = false) List<String> sort
    ) {
        WorkItemFilter filter = new WorkItemFilter(clientName, status, assignee, dueDateFrom, dueDateTo);
        return ResponseEntity.ok(workItemService.findAll(filter, page, size, sort, includeTotal));
    }

    @GetMapping("/{workItemId}")
    @Operation(summary = "업무 단건 조회", description = "업무 ID로 단건을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대상 없음",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ApiResponse<WorkItemResponse>> get(@PathVariable long workItemId) {
        return ResponseEntity.ok(new ApiResponse<>(workItemService.findById(workItemId)));
    }

    @PostMapping
    @Operation(
            summary = "업무 생성",
            description = "신규 업무를 생성합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CreateWorkItemRequest.class),
                            examples = @ExampleObject(
                                    name = "CreateWorkItem",
                                    value = "{\"clientId\":101,\"type\":\"FILING\",\"status\":\"TODO\",\"assignee\":\"kim\",\"dueDate\":\"2025-12-31\",\"tags\":[\"VAT\",\"Q4\"],\"memo\":\"신규 생성\"}"
                            )
                    )
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "도메인 규칙 충돌",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ApiResponse<WorkItemResponse>> create(@Valid @RequestBody CreateWorkItemRequest request) {
        WorkItemResponse created = workItemService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/work-items/" + created.id()))
                .body(new ApiResponse<>(created));
    }

    @PatchMapping("/{workItemId}")
    @Operation(summary = "업무 부분 수정", description = "If-Match 버전 기반으로 업무를 부분 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대상 없음",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "동시성 충돌",
                    content = @Content(
                            schema = @Schema(implementation = ConcurrencyConflictResponse.class),
                            examples = @ExampleObject(
                                    name = "ConcurrencyConflict",
                                    value = "{\"code\":\"CONCURRENT_MODIFICATION\",\"message\":\"Version conflict\",\"clientVersion\":2,\"serverVersion\":3}"
                            )
                    ))
    })
    public ResponseEntity<ApiResponse<WorkItemResponse>> patch(
            @PathVariable long workItemId,
            @Parameter(description = "낙관적 락 버전(ETag). 따옴표 포함 가능", example = "\"3\"")
            @RequestHeader("If-Match") String ifMatch,
            @Valid @RequestBody PatchWorkItemRequest request
    ) {
        long version = parseIfMatchVersion(ifMatch);
        WorkItemResponse updated = workItemService.patch(workItemId, version, request);
        return ResponseEntity.ok().eTag("\"" + updated.version() + "\"")
                .body(new ApiResponse<>(updated));
    }

    @DeleteMapping("/{workItemId}")
    @Operation(summary = "업무 삭제", description = "업무를 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대상 없음",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> delete(@PathVariable long workItemId) {
        workItemService.delete(workItemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{workItemId}/audit-logs")
    @Operation(summary = "업무 이력 조회", description = "업무 변경 이력을 페이징 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = PagedResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대상 없음",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
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

    @PostMapping(path = {":bulk", "/bulk"})
    @Operation(summary = "업무 대량 등록 작업 제출", description = "CSV/배열 데이터 기반 대량 등록 비동기 작업을 제출합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = BulkInsertRequest.class),
                            examples = @ExampleObject(
                                    name = "BulkInsertExample",
                                    value = "{\"requestId\":\"bulk-req-001\",\"items\":[{\"clientId\":101,\"type\":\"FILING\",\"assignee\":\"kim\",\"dueDate\":\"2025-12-31\",\"memo\":\"첫번째\"},{\"clientId\":102,\"type\":\"REVIEW\",\"assignee\":\"lee\",\"dueDate\":\"2025-12-25\",\"memo\":\"두번째\"}]}"
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
    public ResponseEntity<ApiResponse<JobAcceptedResponse>> bulk(
            @Valid @RequestBody BulkInsertRequest request,
            @Parameter(description = "요청 추적 헤더(X-Request-Id)", example = "bulk-20260310-001")
            @RequestHeader(value = RequestIdFilter.REQUEST_ID_HEADER, required = false) String requestIdHeader
    ) {
        String requestId = requestIdHeader == null || requestIdHeader.isBlank() ? request.requestId() : requestIdHeader;
        return ResponseEntity.accepted().body(new ApiResponse<>(workItemService.submitBulk(request, requestId)));
    }

    @PostMapping(path = {":bulk-file", "/bulk-file"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "업무 CSV 대량 등록 작업 제출", description = "CSV 파일을 서버에서 스트리밍 파싱하여 비동기 대량 등록 작업을 제출합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "작업 접수 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ApiResponse<JobAcceptedResponse>> bulkFile(
            @Parameter(description = "업로드 CSV 파일", required = true)
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "요청 ID(선택)", example = "bulk-file-20260310-001")
            @RequestParam(value = "requestId", required = false) String requestIdParam,
            @Parameter(description = "요청 추적 헤더(X-Request-Id)", example = "bulk-file-20260310-001")
            @RequestHeader(value = RequestIdFilter.REQUEST_ID_HEADER, required = false) String requestIdHeader
    ) {
        String requestId = requestIdHeader;
        if (requestId == null || requestId.isBlank()) {
            requestId = requestIdParam;
        }
        return ResponseEntity.accepted().body(new ApiResponse<>(workItemService.submitBulkCsv(file, requestId)));
    }

    @PostMapping(path = {":bulk-file:validate", "/bulk-file/validate"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "업무 CSV 대량 등록 사전검증", description = "업로드 전 CSV의 clientId 유효성/형식 오류를 점검합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검증 완료",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ApiResponse<BulkCsvValidationResponse>> validateBulkFile(
            @Parameter(description = "검증할 CSV 파일", required = true)
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(new ApiResponse<>(workItemService.validateBulkCsv(file)));
    }

    @GetMapping("/bulk-jobs/{jobId}/failures")
    @Operation(summary = "BULK 실패 행 리포트 다운로드", description = "CSV BULK 작업의 실패 행 리포트를 다운로드합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "다운로드 성공",
                    content = @Content(mediaType = "text/csv")),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "리포트 없음",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody> downloadBulkFailureReport(
            @PathVariable String jobId
    ) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header("Content-Disposition", "attachment; filename=\"bulk-failures-" + jobId + ".csv\"")
                .body(workItemService.downloadBulkFailureReport(jobId));
    }

    private long parseIfMatchVersion(String ifMatch) {
        String cleaned = ifMatch.replace("\"", "").trim();
        return Long.parseLong(cleaned);
    }
}
