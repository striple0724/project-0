package com.taxworkbench.api.infrastructure.mybatis.mapper;

import com.taxworkbench.api.infrastructure.mybatis.model.WorkItemGridRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface WorkItemQueryMapper {
    List<WorkItemGridRow> search(
            @Param("clientName") String clientName,
            @Param("status") String status,
            @Param("assignee") String assignee,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    long count(
            @Param("clientName") String clientName,
            @Param("status") String status,
            @Param("assignee") String assignee
    );
}
