package com.taxworkbench.api.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "페이징 응답")
public record PagedResponse<T>(
        @Schema(description = "목록 데이터")
        List<T> data,
        @Schema(description = "페이지 정보")
        PageInfo page
) {
}
