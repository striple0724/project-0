package com.taxworkbench.api.controller;

import com.taxworkbench.api.dto.common.ApiResponse;
import com.taxworkbench.api.dto.workbench.WorkbenchTrackingResponse;
import com.taxworkbench.api.service.WorkbenchTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workbench")
@Tag(name = "Workbench", description = "워크벤치 상태/추적 API")
public class WorkbenchTrackingController {
    private final WorkbenchTrackingService workbenchTrackingService;

    public WorkbenchTrackingController(WorkbenchTrackingService workbenchTrackingService) {
        this.workbenchTrackingService = workbenchTrackingService;
    }

    @GetMapping("/tracking")
    @Operation(summary = "워크벤치 추적 상태 조회", description = "대기 승인/비동기 작업/벌크/내보내기 상태를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "로그인 필요",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ApiResponse<WorkbenchTrackingResponse>> tracking() {
        return ResponseEntity.ok(new ApiResponse<>(workbenchTrackingService.getTrackingStatus()));
    }
}
