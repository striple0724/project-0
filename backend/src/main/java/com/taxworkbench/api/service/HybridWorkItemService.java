package com.taxworkbench.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxworkbench.api.application.port.command.WorkItemCommandStore;
import com.taxworkbench.api.application.port.query.WorkItemQueryStore;
import com.taxworkbench.api.dto.client.ClientFilter;
import com.taxworkbench.api.dto.client.ClientResponse;
import com.taxworkbench.api.dto.client.CreateClientRequest;
import com.taxworkbench.api.dto.common.JobAcceptedResponse;
import com.taxworkbench.api.dto.common.PageInfo;
import com.taxworkbench.api.dto.common.PagedResponse;
import com.taxworkbench.api.dto.workitem.AuditLogResponse;
import com.taxworkbench.api.dto.workitem.BulkInsertRequest;
import com.taxworkbench.api.dto.workitem.CreateWorkItemRequest;
import com.taxworkbench.api.dto.workitem.PatchWorkItemRequest;
import com.taxworkbench.api.dto.workitem.WorkItemFilter;
import com.taxworkbench.api.dto.workitem.WorkItemResponse;
import com.taxworkbench.api.exception.ConcurrencyConflictException;
import com.taxworkbench.api.exception.ResourceNotFoundException;
import com.taxworkbench.api.infrastructure.jpa.entity.ClientEntity;
import com.taxworkbench.api.infrastructure.jpa.entity.WorkItemAuditEntity;
import com.taxworkbench.api.infrastructure.jpa.entity.WorkItemEntity;
import com.taxworkbench.api.infrastructure.jpa.repository.ClientJpaRepository;
import com.taxworkbench.api.infrastructure.jpa.repository.WorkItemAuditJpaRepository;
import com.taxworkbench.api.infrastructure.mybatis.model.WorkItemGridRow;
import com.taxworkbench.api.model.JobType;
import com.taxworkbench.api.model.WorkStatus;
import com.taxworkbench.api.model.WorkType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

@Service
@Transactional
public class HybridWorkItemService implements WorkItemService, ClientService {
    private final JobService jobService;
    private final ObjectMapper objectMapper;
    private final WorkItemCommandStore commandStore;
    private final WorkItemQueryStore queryStore;
    private final ClientJpaRepository clientRepository;
    private final WorkItemAuditJpaRepository auditRepository;

    public HybridWorkItemService(
            JobService jobService,
            ObjectMapper objectMapper,
            WorkItemCommandStore commandStore,
            WorkItemQueryStore queryStore,
            ClientJpaRepository clientRepository,
            WorkItemAuditJpaRepository auditRepository
    ) {
        this.jobService = jobService;
        this.objectMapper = objectMapper;
        this.commandStore = commandStore;
        this.queryStore = queryStore;
        this.clientRepository = clientRepository;
        this.auditRepository = auditRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<WorkItemResponse> findAll(WorkItemFilter filter, int page, int size, List<String> sort) {
        int offset = Math.max(page, 0) * Math.max(size, 1);
        String status = filter.status() == null ? null : filter.status().name();

        List<WorkItemGridRow> rows = queryStore.search(filter.clientName(), status, filter.assignee(), offset, size);
        long total = queryStore.count(filter.clientName(), status, filter.assignee());

        List<WorkItemResponse> content = rows.stream().map(this::toResponse).toList();
        int totalPages = (int) Math.ceil(total / (double) Math.max(size, 1));
        return new PagedResponse<>(content, new PageInfo(page, size, total, totalPages));
    }

    @Override
    @Transactional(readOnly = true)
    public WorkItemResponse findById(long id) {
        WorkItemEntity entity = commandStore.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkItem not found: " + id));
        return toResponse(entity);
    }

    @Override
    public WorkItemResponse create(CreateWorkItemRequest request) {
        ClientEntity client = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found: " + request.clientId()));

        WorkItemEntity entity = new WorkItemEntity();
        entity.setClient(client);
        entity.setBizNo(client.getBizNo());
        entity.setType(request.type());
        entity.setStatus(request.status() == null ? WorkStatus.TODO : request.status());
        entity.setAssignee(request.assignee());
        entity.setDueDate(request.dueDate());
        entity.setMemo(request.memo());
        entity.setTagsJson(toJson(request.tags()));

        WorkItemEntity saved = commandStore.save(entity);
        saveAudit(saved.getId(), "CREATE", null, "CREATED", request.assignee());
        return toResponse(saved);
    }

    @Override
    public WorkItemResponse patch(long id, long version, PatchWorkItemRequest request) {
        WorkItemEntity entity = commandStore.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkItem not found: " + id));

        if (entity.getVersion() != null && !entity.getVersion().equals(version)) {
            throw new ConcurrencyConflictException(
                    "리소스가 다른 사용자에 의해 변경되었습니다.",
                    version,
                    entity.getVersion(),
                    toResponse(entity)
            );
        }

        String actor = request.assignee() == null || request.assignee().isBlank() ? "system" : request.assignee();
        if (request.status() != null && request.status() != entity.getStatus()) {
            saveAudit(id, "status", entity.getStatus().name(), request.status().name(), actor);
            entity.setStatus(request.status());
        }
        if (request.dueDate() != null && !request.dueDate().equals(entity.getDueDate())) {
            saveAudit(id, "dueDate", String.valueOf(entity.getDueDate()), String.valueOf(request.dueDate()), actor);
            entity.setDueDate(request.dueDate());
        }
        if (request.assignee() != null && !request.assignee().equals(entity.getAssignee())) {
            saveAudit(id, "assignee", entity.getAssignee(), request.assignee(), actor);
            entity.setAssignee(request.assignee());
        }
        if (request.memo() != null && !request.memo().equals(entity.getMemo())) {
            saveAudit(id, "memo", entity.getMemo(), request.memo(), actor);
            entity.setMemo(request.memo());
        }
        if (request.tags() != null) {
            String nextTagsJson = toJson(request.tags());
            if (!nextTagsJson.equals(entity.getTagsJson())) {
                saveAudit(id, "tags", entity.getTagsJson(), nextTagsJson, actor);
                entity.setTagsJson(nextTagsJson);
            }
        }

        WorkItemEntity saved = commandStore.save(entity);
        return toResponse(saved);
    }

    @Override
    public void delete(long id) {
        WorkItemEntity entity = commandStore.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkItem not found: " + id));
        commandStore.deleteById(entity.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<AuditLogResponse> findAuditLogs(long id, String field, String from, String to, int page, int size) {
        if (commandStore.findById(id).isEmpty()) {
            throw new ResourceNotFoundException("WorkItem not found: " + id);
        }
        Pageable pageable = PageRequest.of(page, size);
        var result = auditRepository.findByWorkItemIdOrderByChangedAtDesc(id, pageable);

        List<AuditLogResponse> rows = result.getContent().stream()
                .filter(a -> field == null || field.isBlank() || field.equalsIgnoreCase(a.getFieldName()))
                .map(a -> new AuditLogResponse(
                        a.getId(),
                        a.getWorkItemId(),
                        a.getFieldName(),
                        a.getBeforeValue(),
                        a.getAfterValue(),
                        a.getChangedBy(),
                        a.getChangedAt()
                ))
                .toList();

        return new PagedResponse<>(rows, new PageInfo(page, size, result.getTotalElements(), result.getTotalPages()));
    }

    @Override
    public JobAcceptedResponse submitBulk(BulkInsertRequest request, String requestId) {
        return jobService.createJob(requestId, JobType.BULK_INSERT);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ClientResponse> findAll(ClientFilter filter, int page, int size) {
        Specification<ClientEntity> spec = (root, query, cb) -> cb.conjunction();
        if (filter != null) {
            if (filter.name() != null && !filter.name().isBlank()) {
                spec = spec.and((root, query, cb) -> cb.like(root.get("name"), "%" + filter.name() + "%"));
            }
            if (filter.type() != null) {
                spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), filter.type()));
            }
            if (filter.tier() != null) {
                spec = spec.and((root, query, cb) -> cb.equal(root.get("tier"), filter.tier()));
            }
            if (filter.status() != null) {
                spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), filter.status()));
            }
        }

        var pageResult = clientRepository.findAll(spec, PageRequest.of(page, size));
        List<ClientResponse> clients = pageResult.getContent().stream()
                .map(c -> new ClientResponse(c.getId(), c.getName(), c.getBizNo(), c.getType(), c.getStatus(), c.getTier()))
                .toList();

        return new PagedResponse<>(clients, new PageInfo(page, size, pageResult.getTotalElements(), pageResult.getTotalPages()));
    }

    @Override
    public ClientResponse create(CreateClientRequest request) {
        ClientEntity entity = new ClientEntity();
        entity.setName(request.name());
        entity.setBizNo(request.bizNo());
        entity.setType(request.type());
        entity.setStatus(request.status());
        entity.setTier(request.tier());
        ClientEntity saved = clientRepository.save(entity);
        return new ClientResponse(saved.getId(), saved.getName(), saved.getBizNo(), saved.getType(), saved.getStatus(), saved.getTier());
    }

    private void saveAudit(Long workItemId, String field, String beforeValue, String afterValue, String changedBy) {
        WorkItemAuditEntity audit = new WorkItemAuditEntity();
        audit.setWorkItemId(workItemId);
        audit.setFieldName(field);
        audit.setBeforeValue(beforeValue);
        audit.setAfterValue(afterValue);
        audit.setChangedBy(changedBy == null || changedBy.isBlank() ? "system" : changedBy);
        auditRepository.save(audit);
    }

    private WorkItemResponse toResponse(WorkItemEntity entity) {
        return new WorkItemResponse(
                entity.getId(),
                entity.getClient().getId(),
                entity.getClient().getName(),
                entity.getBizNo(),
                entity.getType(),
                entity.getStatus(),
                entity.getAssignee(),
                entity.getDueDate(),
                parseTags(entity.getTagsJson()),
                entity.getMemo(),
                entity.getVersion(),
                entity.getUpdatedAt()
        );
    }

    private WorkItemResponse toResponse(WorkItemGridRow row) {
        return new WorkItemResponse(
                row.id(),
                row.clientId(),
                row.clientName(),
                row.bizNo(),
                parseWorkType(row.type()),
                parseWorkStatus(row.status()),
                row.assignee(),
                LocalDate.parse(row.dueDate()),
                parseTags(row.tagsJson()),
                row.memo(),
                row.version(),
                OffsetDateTime.parse(row.updatedAt())
        );
    }

    private WorkStatus parseWorkStatus(String value) {
        return value == null ? WorkStatus.TODO : WorkStatus.valueOf(value);
    }

    private WorkType parseWorkType(String value) {
        return value == null ? WorkType.ETC : WorkType.valueOf(value);
    }

    private String toJson(List<String> tags) {
        try {
            return objectMapper.writeValueAsString(tags == null ? Collections.emptyList() : tags);
        } catch (Exception e) {
            return "[]";
        }
    }

    private List<String> parseTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(tagsJson, new TypeReference<>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }
}
