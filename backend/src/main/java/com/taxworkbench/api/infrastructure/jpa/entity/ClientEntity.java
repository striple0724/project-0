package com.taxworkbench.api.infrastructure.jpa.entity;

import com.taxworkbench.api.model.ClientStatus;
import com.taxworkbench.api.model.ClientTier;
import com.taxworkbench.api.model.ClientType;
import jakarta.persistence.*;

@Entity
@Table(name = "clients")
public class ClientEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 30)
    private String bizNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClientType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClientStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClientTier tier;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBizNo() { return bizNo; }
    public void setBizNo(String bizNo) { this.bizNo = bizNo; }
    public ClientType getType() { return type; }
    public void setType(ClientType type) { this.type = type; }
    public ClientStatus getStatus() { return status; }
    public void setStatus(ClientStatus status) { this.status = status; }
    public ClientTier getTier() { return tier; }
    public void setTier(ClientTier tier) { this.tier = tier; }
}
