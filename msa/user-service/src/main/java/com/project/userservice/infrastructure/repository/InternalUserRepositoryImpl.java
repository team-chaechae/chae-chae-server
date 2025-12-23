package com.project.userservice.infrastructure.repository;

import com.project.userservice.domain.model.InternalUserEntity;
import com.project.userservice.domain.repository.InternalUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class InternalUserRepositoryImpl implements InternalUserRepository {

    private final JpaInternalUserRepository jpaInternalUserRepository;

    @Override
    public Optional<InternalUserEntity> findByEmailDeletedAtIsNull(String email) {
        return jpaInternalUserRepository.findByEmailAndDeletedAtIsNull(email);
    }

    @Override
    public Optional<InternalUserEntity> findByEmail(String email) {
        return jpaInternalUserRepository.findByEmail(email);
    }

    @Override
    public void save(InternalUserEntity InternalUserEntity) {
        jpaInternalUserRepository.save(InternalUserEntity);
    }
}
