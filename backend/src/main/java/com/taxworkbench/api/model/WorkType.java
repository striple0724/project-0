package com.taxworkbench.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "업무 유형")
public enum WorkType {
    FILING,
    BOOKKEEPING,
    REVIEW,
    ETC
}
