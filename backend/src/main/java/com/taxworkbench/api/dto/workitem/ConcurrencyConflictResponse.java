package com.taxworkbench.api.dto.workitem;

public record ConcurrencyConflictResponse(
        String code,
        String message,
        long clientVersion,
        long serverVersion,
        WorkItemResponse serverSnapshot
) {
}
