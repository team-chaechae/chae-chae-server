package com.project.chaechaeserver.application.service.user;

import com.project.chaechaeserver.application.response.user.ResInternalUserPostSignupDTO;
import com.project.chaechaeserver.domain.model.user.InternalUserEntity;
import com.project.chaechaeserver.domain.model.user.constraint.PositionType;
import com.project.chaechaeserver.domain.model.user.constraint.RoleType;
import com.project.chaechaeserver.domain.repository.user.InternalUserRepository;
import com.project.chaechaeserver.presentation.request.user.ReqInternalUserPostSignupDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InternalUserService {

    private final InternalUserRepository internalUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ResInternalUserPostSignupDTO signup(ReqInternalUserPostSignupDTO dto) {

        // -- 이메일 중복확인 -- //
        internalUserRepository.findByEmailDeletedAtIsNull(dto.getInternalUser().getEmail())
                .ifPresent(internalUser -> {
                    throw new IllegalArgumentException("이미 존재하는 이메일입니다.");
                });

        // -- 회원 생성 -- //
        InternalUserEntity savingForInternalUserEntity = InternalUserEntity.createInternalUser(
                dto.getInternalUser().getEmail(),
                passwordEncoder.encode(dto.getInternalUser().getPassword()),
                dto.getInternalUser().getRealName(),
                PositionType.valueOf(dto.getInternalUser().getPosition()),
                RoleType.valueOf(dto.getInternalUser().getRole())
        );

        // -- 회원 저장 -- //
        internalUserRepository.save(savingForInternalUserEntity);

        return ResInternalUserPostSignupDTO.of(savingForInternalUserEntity);
    }
}
