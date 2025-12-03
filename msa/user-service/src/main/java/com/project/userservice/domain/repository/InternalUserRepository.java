package com.project.userservice.domain.repository;

import com.project.userservice.domain.model.InternalUserEntity;

import java.util.Optional;

public interface InternalUserRepository {

    Optional<InternalUserEntity> findByEmailDeletedAtIsNull(String email);

    Optional<InternalUserEntity> findByEmail(String email);

    void save(InternalUserEntity InternalUserEntity);
}
