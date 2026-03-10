package com.taxworkbench.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "고객사 유형")
public enum ClientType {
    INDIVIDUAL,
    CORPORATE
}
