package com.taxworkbench.api.dto.auth;

public record LoginResponse(
        long seq,
        String userId,
        String name,
        String useYn
) {
}
