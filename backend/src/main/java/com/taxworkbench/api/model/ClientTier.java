package com.taxworkbench.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "고객사 등급")
public enum ClientTier {
    BASIC,
    PREMIUM,
    VIP
}
