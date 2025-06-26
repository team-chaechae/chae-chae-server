package com.project.chaechaeserver.infrastructure.user;

import com.project.chaechaeserver.domain.model.user.InternalUserEntity;
import com.project.chaechaeserver.domain.repository.user.InternalUserRepository;
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
    public void save(InternalUserEntity InternalUserEntity) {
        jpaInternalUserRepository.save(InternalUserEntity);
    }
}