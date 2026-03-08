package com.taxworkbench.api.infrastructure.mybatis.model;

public record WorkItemGridRow(
        Long id,
        Long clientId,
        String clientName,
        String bizNo,
        String type,
        String status,
        String assignee,
        String dueDate,
        String tagsJson,
        String memo,
        Long version,
        String updatedAt
) {
}
