package com.taxworkbench.api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Size(max = 20) String userId,
        @NotBlank @Size(max = 200) String password
) {
}
