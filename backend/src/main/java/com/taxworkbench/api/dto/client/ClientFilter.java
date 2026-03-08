package com.taxworkbench.api.dto.client;

import com.taxworkbench.api.model.ClientStatus;
import com.taxworkbench.api.model.ClientTier;
import com.taxworkbench.api.model.ClientType;

public record ClientFilter(
        String name,
        ClientType type,
        ClientTier tier,
        ClientStatus status
) {
}
