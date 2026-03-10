package com.taxworkbench.api.dto.client;

import com.taxworkbench.api.model.ClientStatus;
import com.taxworkbench.api.model.ClientTier;
import com.taxworkbench.api.model.ClientType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "고객사 수정 요청")
public record UpdateClientRequest(
        @Schema(description = "고객사명", example = "ABC 세무법인")
        @NotBlank String name,
        @Schema(description = "사업자등록번호", example = "123-45-67890")
        String bizNo,
        @Schema(description = "고객사 유형", implementation = ClientType.class, example = "CORPORATE")
        @NotNull ClientType type,
        @Schema(description = "고객사 상태", implementation = ClientStatus.class, example = "ACTIVE")
        @NotNull ClientStatus status,
        @Schema(description = "고객사 등급", implementation = ClientTier.class, example = "PREMIUM")
        @NotNull ClientTier tier
) {
}
