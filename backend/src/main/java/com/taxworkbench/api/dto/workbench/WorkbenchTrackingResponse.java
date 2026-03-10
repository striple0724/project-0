package com.taxworkbench.api.dto.workbench;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "워크벤치 추적 요약")
public record WorkbenchTrackingResponse(
        @Schema(description = "대기 승인 건수", example = "3")
        long pendingApprovalCount,
        @Schema(description = "비동기 작업 건수", example = "8")
        long asyncJobCount,
        @Schema(description = "벌크 비동기 건수", example = "5")
        long bulkAsyncCount,
        @Schema(description = "내보내기 비동기 건수", example = "3")
        long exportAsyncCount,
        @Schema(description = "최근 작업 목록")
        List<JobTrackingItemResponse> recentJobs
) {
}
