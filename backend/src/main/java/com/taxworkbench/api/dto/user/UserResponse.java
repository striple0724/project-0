package com.taxworkbench.api.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "사용자 응답")
public record UserResponse(
        @Schema(description = "사용자 SEQ", example = "1")
        Long seq,
        @Schema(description = "로그인 ID", example = "admin")
        String id,
        @Schema(description = "이름", example = "관리자")
        String name,
        @Schema(description = "이메일", example = "admin@example.com")
        String email,
        @Schema(description = "휴대폰번호", example = "01000000000")
        String mobileNo,
        @Schema(description = "사용 여부(Y/N)", example = "Y")
        String useYn,
        @Schema(description = "생성자", example = "system")
        String crtBy,
        @Schema(description = "생성 일시")
        LocalDateTime crtDt,
        @Schema(description = "생성 IP", example = "127.0.0.1")
        String crtIp,
        @Schema(description = "수정자", example = "admin")
        String amnBy,
        @Schema(description = "수정 일시")
        LocalDateTime amnDt,
        @Schema(description = "수정 IP", example = "127.0.0.1")
        String amnIp
) {
}
