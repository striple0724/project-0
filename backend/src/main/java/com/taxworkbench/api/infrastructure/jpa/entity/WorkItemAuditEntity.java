package com.taxworkbench.api.infrastructure.jpa.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "work_item_audits")
public class WorkItemAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long workItemId;

    @Column(nullable = false, length = 100)
    private String fieldName;

    @Column(length = 2000)
    private String beforeValue;

    @Column(length = 2000)
    private String afterValue;

    @Column(nullable = false, length = 100)
    private String changedBy;

    @Column(nullable = false)
    private OffsetDateTime changedAt;

    @PrePersist
    void stamp() {
        if (changedAt == null) {
            changedAt = OffsetDateTime.now();
        }
    }

    public Long getId() { return id; }
    public Long getWorkItemId() { return workItemId; }
    public void setWorkItemId(Long workItemId) { this.workItemId = workItemId; }
    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public String getBeforeValue() { return beforeValue; }
    public void setBeforeValue(String beforeValue) { this.beforeValue = beforeValue; }
    public String getAfterValue() { return afterValue; }
    public void setAfterValue(String afterValue) { this.afterValue = afterValue; }
    public String getChangedBy() { return changedBy; }
    public void setChangedBy(String changedBy) { this.changedBy = changedBy; }
    public OffsetDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(OffsetDateTime changedAt) { this.changedAt = changedAt; }
}
