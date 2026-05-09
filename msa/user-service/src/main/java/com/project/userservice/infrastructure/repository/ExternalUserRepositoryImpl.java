package com.project.userservice.infrastructure.repository;

import com.project.userservice.domain.model.ExternalUserEntity;
import com.project.userservice.domain.repository.ExternalUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ExternalUserRepositoryImpl implements ExternalUserRepository {

    private final JpaExternalUserRepository jpaExternalUserRepository;

    @Override
    public Optional<ExternalUserEntity> findByEmailAndDeletedAtIsNull(String email) {
        return jpaExternalUserRepository.findByEmailAndDeletedAtIsNull(email);
    }

    @Override
    public Optional<ExternalUserEntity> findByEmail(String email) {
        return jpaExternalUserRepository.findByEmail(email);
    }

    @Override
    public Optional<ExternalUserEntity> findById(Long id) {
        return jpaExternalUserRepository.findById(id);
    }

    @Override
    public ExternalUserEntity save(ExternalUserEntity externalUserEntity) {
        return jpaExternalUserRepository.save(externalUserEntity);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaExternalUserRepository.existsByEmailAndDeletedAtIsNull(email);
    }
}
