package com.taxworkbench.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "고객사 상태")
public enum ClientStatus {
    ACTIVE,
    INACTIVE
}
