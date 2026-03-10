package com.taxworkbench.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "비동기 작업 유형")
public enum JobType {
    BULK_INSERT,
    EXPORT
}
