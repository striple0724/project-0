package com.taxworkbench.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "로그인 요청")
public record LoginRequest(
        @Schema(description = "사용자 ID", example = "admin")
        @NotBlank @Size(max = 20) String userId,
        @Schema(description = "비밀번호", example = "admin1234")
        @NotBlank @Size(max = 200) String password
) {
}
