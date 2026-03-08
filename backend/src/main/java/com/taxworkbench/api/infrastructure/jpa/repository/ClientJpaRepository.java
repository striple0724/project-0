package com.taxworkbench.api.infrastructure.jpa.repository;

import com.taxworkbench.api.infrastructure.jpa.entity.ClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ClientJpaRepository extends JpaRepository<ClientEntity, Long>, JpaSpecificationExecutor<ClientEntity> {
}
