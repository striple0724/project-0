package com.taxworkbench.api.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "페이지 메타 정보")
public record PageInfo(
        @Schema(description = "현재 페이지 번호(0-base)", example = "0")
        int number,
        @Schema(description = "페이지 크기", example = "50")
        int size,
        @Schema(description = "전체 건수", example = "1234")
        long totalElements,
        @Schema(description = "전체 페이지 수", example = "25")
        int totalPages
) {
}
