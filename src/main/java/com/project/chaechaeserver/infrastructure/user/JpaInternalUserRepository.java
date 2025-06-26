package com.project.chaechaeserver.infrastructure.user;

import com.project.chaechaeserver.domain.model.user.InternalUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JpaInternalUserRepository extends JpaRepository<InternalUserEntity, Long> {

    Optional<InternalUserEntity> findByEmailAndDeletedAtIsNull(String email);
}