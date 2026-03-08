package com.taxworkbench.api.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(max = 20) String id,
        @NotBlank @Size(max = 20) String name,
        @NotBlank @Size(max = 200) String password,
        @NotBlank @Size(max = 20) String email,
        @NotBlank @Size(max = 20) String mobileNo,
        @NotBlank @Size(max = 1) String useYn,
        @NotBlank @Size(max = 20) String crtBy,
        @NotBlank @Size(max = 20) String crtIp
) {
}
