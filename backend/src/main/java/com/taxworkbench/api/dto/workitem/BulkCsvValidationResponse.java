package com.taxworkbench.api.dto.workitem;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "BULK CSV 사전검증 결과")
public record BulkCsvValidationResponse(
        @Schema(description = "전체 데이터 행 수(헤더 제외)", example = "100000")
        int totalRows,
        @Schema(description = "정상 처리 예상 행 수", example = "7500")
        int validRows,
        @Schema(description = "실패 예상 행 수", example = "92500")
        int invalidRows,
        @Schema(description = "유니크 clientId 수", example = "40")
        int uniqueClientIdCount,
        @Schema(description = "존재하지 않는 clientId 수", example = "37")
        int missingClientIdCount,
        @Schema(description = "존재하지 않는 clientId 목록(최대 100개)")
        List<Long> missingClientIds,
        @Schema(description = "형식 오류 행 수(clientId/컬럼/날짜 포맷 등)", example = "0")
        int malformedRows
) {
}
