package com.taxworkbench.api.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "사용자 수정 요청")
public record UpdateUserRequest(
        @Schema(description = "이름", example = "홍길동")
        @NotBlank @Size(max = 20) String name,
        @Schema(description = "비밀번호(변경 시 입력)", example = "P@ssw0rd!")
        @Size(max = 200) String password,
        @Schema(description = "이메일", example = "hong@example.com")
        @NotBlank @Size(max = 20) String email,
        @Schema(description = "휴대폰번호", example = "01012345678")
        @NotBlank @Size(max = 20) String mobileNo,
        @Schema(description = "사용 여부(Y/N)", example = "Y")
        @NotBlank @Size(max = 1) String useYn,
        @Schema(description = "수정자", example = "admin")
        @NotBlank @Size(max = 20) String amnBy,
        @Schema(description = "수정 IP", example = "127.0.0.1")
        @NotBlank @Size(max = 20) String amnIp
) {
}
