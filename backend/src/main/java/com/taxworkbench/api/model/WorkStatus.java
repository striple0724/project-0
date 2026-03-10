package com.taxworkbench.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "업무 상태")
public enum WorkStatus {
    TODO,
    IN_PROGRESS,
    DONE,
    HOLD
}
