package com.taxworkbench.api.infrastructure.jpa.entity;

import com.taxworkbench.api.model.WorkStatus;
import com.taxworkbench.api.model.WorkType;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "work_items")
public class WorkItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity client;

    @Column(length = 30)
    private String bizNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private WorkStatus status;

    @Column(nullable = false, length = 100)
    private String assignee;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(length = 1000)
    private String tagsJson;

    @Column(length = 2000)
    private String memo;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public ClientEntity getClient() { return client; }
    public void setClient(ClientEntity client) { this.client = client; }
    public String getBizNo() { return bizNo; }
    public void setBizNo(String bizNo) { this.bizNo = bizNo; }
    public WorkType getType() { return type; }
    public void setType(WorkType type) { this.type = type; }
    public WorkStatus getStatus() { return status; }
    public void setStatus(WorkStatus status) { this.status = status; }
    public String getAssignee() { return assignee; }
    public void setAssignee(String assignee) { this.assignee = assignee; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getTagsJson() { return tagsJson; }
    public void setTagsJson(String tagsJson) { this.tagsJson = tagsJson; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
    public Long getVersion() { return version; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
