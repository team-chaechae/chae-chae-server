package com.project.userservice.infrastructure.repository;

import com.project.userservice.domain.model.InternalUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaInternalUserRepository extends JpaRepository<InternalUserEntity, Long> {

    Optional<InternalUserEntity> findByEmailAndDeletedAtIsNull(String email);

    Optional<InternalUserEntity> findByEmail(String email);
}
