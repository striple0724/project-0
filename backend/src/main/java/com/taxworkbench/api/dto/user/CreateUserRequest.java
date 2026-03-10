package com.taxworkbench.api.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "사용자 생성 요청")
public record CreateUserRequest(
        @Schema(description = "로그인 ID", example = "honggildong")
        @NotBlank @Size(max = 20) String id,
        @Schema(description = "이름", example = "홍길동")
        @NotBlank @Size(max = 20) String name,
        @Schema(description = "비밀번호", example = "P@ssw0rd!")
        @NotBlank @Size(max = 200) String password,
        @Schema(description = "이메일", example = "hong@example.com")
        @NotBlank @Size(max = 20) String email,
        @Schema(description = "휴대폰번호", example = "01012345678")
        @NotBlank @Size(max = 20) String mobileNo,
        @Schema(description = "사용 여부(Y/N)", example = "Y")
        @NotBlank @Size(max = 1) String useYn,
        @Schema(description = "생성자", example = "admin")
        @NotBlank @Size(max = 20) String crtBy,
        @Schema(description = "생성 IP", example = "127.0.0.1")
        @NotBlank @Size(max = 20) String crtIp
) {
}
