package com.taxworkbench.api.exception;

import com.taxworkbench.api.dto.workitem.WorkItemResponse;

public class ConcurrencyConflictException extends RuntimeException {
    private final long clientVersion;
    private final long serverVersion;
    private final WorkItemResponse serverSnapshot;

    public ConcurrencyConflictException(String message, long clientVersion, long serverVersion, WorkItemResponse serverSnapshot) {
        super(message);
        this.clientVersion = clientVersion;
        this.serverVersion = serverVersion;
        this.serverSnapshot = serverSnapshot;
    }

    public long getClientVersion() {
        return clientVersion;
    }

    public long getServerVersion() {
        return serverVersion;
    }

    public WorkItemResponse getServerSnapshot() {
        return serverSnapshot;
    }
}
