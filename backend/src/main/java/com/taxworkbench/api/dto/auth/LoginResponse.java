package com.taxworkbench.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 응답")
public record LoginResponse(
        @Schema(description = "사용자 일련번호", example = "1")
        long seq,
        @Schema(description = "사용자 ID", example = "admin")
        String userId,
        @Schema(description = "사용자명", example = "관리자")
        String name,
        @Schema(description = "사용 여부(Y/N)", example = "Y")
        String useYn
) {
}
