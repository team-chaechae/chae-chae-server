package com.project.chaechaeserver.infrastructure.security;

import com.project.chaechaeserver.domain.model.user.InternalUserEntity;
import com.project.chaechaeserver.domain.repository.user.InternalUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final InternalUserRepository internalUserRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        InternalUserEntity internalUserEntity = internalUserRepository.findByEmailDeletedAtIsNull(email)
                .orElseThrow(() -> new UsernameNotFoundException("이메일을 정확히 입력해주세요."));

        return CustomUserDetails.of(internalUserEntity);
    }
}