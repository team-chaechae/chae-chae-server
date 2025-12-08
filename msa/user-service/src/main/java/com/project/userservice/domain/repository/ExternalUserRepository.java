package com.project.userservice.domain.repository;

import com.project.userservice.domain.model.ExternalUserEntity;

import java.util.Optional;

public interface ExternalUserRepository {

    Optional<ExternalUserEntity> findByEmailAndDeletedAtIsNull(String email);

    Optional<ExternalUserEntity> findByEmail(String email);

    Optional<ExternalUserEntity> findById(Long id);

    ExternalUserEntity save(ExternalUserEntity externalUserEntity);

    boolean existsByEmail(String email);
}
