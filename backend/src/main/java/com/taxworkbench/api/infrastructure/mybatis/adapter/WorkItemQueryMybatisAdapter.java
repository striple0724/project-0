package com.taxworkbench.api.infrastructure.mybatis.adapter;

import com.taxworkbench.api.application.port.query.WorkItemQueryStore;
import com.taxworkbench.api.infrastructure.mybatis.mapper.WorkItemQueryMapper;
import com.taxworkbench.api.infrastructure.mybatis.model.WorkItemGridRow;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class WorkItemQueryMybatisAdapter implements WorkItemQueryStore {
    private final WorkItemQueryMapper mapper;

    public WorkItemQueryMybatisAdapter(WorkItemQueryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<WorkItemGridRow> search(
            String clientName,
            String status,
            String assignee,
            String dueDateFrom,
            String dueDateTo,
            String sortField,
            String sortDirection,
            int offset,
            int limit
    ) {
        return mapper.search(clientName, status, assignee, dueDateFrom, dueDateTo, sortField, sortDirection, offset, limit);
    }

    @Override
    public long count(String clientName, String status, String assignee, String dueDateFrom, String dueDateTo) {
        return mapper.count(clientName, status, assignee, dueDateFrom, dueDateTo);
    }
}
