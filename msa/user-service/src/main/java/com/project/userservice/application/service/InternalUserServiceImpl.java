package com.project.userservice.application.service;

import com.project.userservice.application.global.exception.EntityAlreadyExistException;
import com.project.userservice.application.response.ResInternalUserPostSignupDTO;
import com.project.userservice.domain.model.InternalUserEntity;
import com.project.userservice.domain.model.constraint.PositionType;
import com.project.userservice.domain.repository.InternalUserRepository;
import com.project.userservice.presentation.request.ReqInternalUserPostSignupDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static com.project.userservice.domain.model.constraint.RoleType.EMPLOYEE;

@Service
@RequiredArgsConstructor
public class InternalUserServiceImpl implements InternalUserService {

    private final InternalUserRepository internalUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public ResInternalUserPostSignupDTO signup(ReqInternalUserPostSignupDTO dto) {

        // -- 이메일 중복확인 -- //
        internalUserRepository.findByEmailDeletedAtIsNull(dto.getInternalUser().getEmail())
                .ifPresent(internalUser -> {
                    throw new EntityAlreadyExistException("이미 존재하는 이메일입니다.");
                });

        // -- 회원 생성 -- //
        InternalUserEntity savingForInternalUserEntity = InternalUserEntity.createInternalUser(
                dto.getInternalUser().getEmail(),
                passwordEncoder.encode(dto.getInternalUser().getPassword()),
                dto.getInternalUser().getRealName(),
                dto.getInternalUser().getEmployeeCode(),
                PositionType.from(dto.getInternalUser().getPosition()),
                EMPLOYEE // --> default 로 EMPLOYEE 권한 설정
        );

        // -- 회원 저장 -- //
        internalUserRepository.save(savingForInternalUserEntity);

        return ResInternalUserPostSignupDTO.from(savingForInternalUserEntity);
    }
}
