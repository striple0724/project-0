package com.taxworkbench.api.dto.client;

import com.taxworkbench.api.model.ClientStatus;
import com.taxworkbench.api.model.ClientTier;
import com.taxworkbench.api.model.ClientType;

public record ClientResponse(
        Long id,
        String name,
        String bizNo,
        ClientType type,
        ClientStatus status,
        ClientTier tier
) {
}
