package com.project.chaechaeserver.domain.repository.user;

import com.project.chaechaeserver.domain.model.user.InternalUserEntity;

import java.util.Optional;

public interface InternalUserRepository {

    Optional<InternalUserEntity> findByEmailDeletedAtIsNull(String email);

    void save(InternalUserEntity InternalUserEntity);
}