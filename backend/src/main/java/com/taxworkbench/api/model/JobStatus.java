package com.taxworkbench.api.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "비동기 작업 상태")
public enum JobStatus {
    QUEUED,
    RUNNING,
    DONE,
    FAILED
}
