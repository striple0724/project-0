package com.taxworkbench.api.application.port.query;

import com.taxworkbench.api.infrastructure.mybatis.model.WorkItemGridRow;

import java.util.List;

public interface WorkItemQueryStore {
    List<WorkItemGridRow> search(
            String clientName,
            String status,
            String assignee,
            String dueDateFrom,
            String dueDateTo,
            String sortField,
            String sortDirection,
            int offset,
            int limit
    );

    long count(String clientName, String status, String assignee, String dueDateFrom, String dueDateTo);
}
