package com.taxworkbench.api.dto.client;

import com.taxworkbench.api.model.ClientStatus;
import com.taxworkbench.api.model.ClientTier;
import com.taxworkbench.api.model.ClientType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateClientRequest(
        @NotBlank String name,
        String bizNo,
        @NotNull ClientType type,
        @NotNull ClientStatus status,
        @NotNull ClientTier tier
) {
}
