package com.taxworkbench.api.dto.client;

import com.taxworkbench.api.model.ClientStatus;
import com.taxworkbench.api.model.ClientTier;
import com.taxworkbench.api.model.ClientType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "고객사 응답")
public record ClientResponse(
        @Schema(description = "고객사 ID", example = "101")
        Long id,
        @Schema(description = "고객사명", example = "ABC 세무법인")
        String name,
        @Schema(description = "사업자등록번호", example = "123-45-67890")
        String bizNo,
        @Schema(description = "고객사 유형", implementation = ClientType.class, example = "CORPORATE")
        ClientType type,
        @Schema(description = "고객사 상태", implementation = ClientStatus.class, example = "ACTIVE")
        ClientStatus status,
        @Schema(description = "고객사 등급", implementation = ClientTier.class, example = "PREMIUM")
        ClientTier tier
) {
}
