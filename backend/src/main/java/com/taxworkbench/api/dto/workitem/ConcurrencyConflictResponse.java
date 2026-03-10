package com.taxworkbench.api.dto.workitem;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "동시성 충돌 응답")
public record ConcurrencyConflictResponse(
        @Schema(description = "오류 코드", example = "CONFLICT")
        String code,
        @Schema(description = "오류 메시지", example = "버전 충돌이 발생했습니다.")
        String message,
        @Schema(description = "클라이언트 버전", example = "2")
        long clientVersion,
        @Schema(description = "서버 버전", example = "3")
        long serverVersion,
        @Schema(description = "서버 최신 스냅샷")
        WorkItemResponse serverSnapshot
) {
}
