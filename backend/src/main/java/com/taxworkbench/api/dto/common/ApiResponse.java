package com.taxworkbench.api.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공통 API 응답 래퍼")
public record ApiResponse<T>(T data) {
}
