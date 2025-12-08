package com.project.userservice.infrastructure.repository;

import com.project.userservice.domain.model.ExternalUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaExternalUserRepository extends JpaRepository<ExternalUserEntity, Long> {

    Optional<ExternalUserEntity> findByEmailAndDeletedAtIsNull(String email);

    Optional<ExternalUserEntity> findByEmail(String email);

    boolean existsByEmailAndDeletedAtIsNull(String email);
}
