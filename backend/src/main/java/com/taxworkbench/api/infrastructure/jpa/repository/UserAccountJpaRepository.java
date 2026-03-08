package com.taxworkbench.api.infrastructure.jpa.repository;

import com.taxworkbench.api.infrastructure.jpa.entity.UserAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountJpaRepository extends JpaRepository<UserAccountEntity, Long> {
    Optional<UserAccountEntity> findById(String id);

    boolean existsById(String id);
}
