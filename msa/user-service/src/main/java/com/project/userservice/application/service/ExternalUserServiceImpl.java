package com.project.userservice.application.service;

import com.project.userservice.application.global.exception.EntityAlreadyExistException;
import com.project.userservice.application.global.exception.EntityNotFoundException;
import com.project.userservice.application.response.ResExternalUserDTO;
import com.project.userservice.application.response.ResExternalUserSignupDTO;
import com.project.userservice.domain.model.ExternalUserEntity;
import com.project.userservice.domain.repository.ExternalUserRepository;
import com.project.userservice.presentation.request.ReqExternalUserSignupDTO;
import com.project.userservice.presentation.request.ReqExternalUserUpdateDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalUserServiceImpl implements ExternalUserService {

    private final ExternalUserRepository externalUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public ResExternalUserSignupDTO signup(ReqExternalUserSignupDTO dto) {
        ReqExternalUserSignupDTO.ExternalUser user = dto.getExternalUser();

        // 이메일 중복 확인
        if (externalUserRepository.existsByEmail(user.getEmail())) {
            throw new EntityAlreadyExistException("이미 존재하는 이메일입니다.");
        }

        // 고객 회원 생성
        ExternalUserEntity newUser = ExternalUserEntity.createCustomer(
                user.getEmail(),
                passwordEncoder.encode(user.getPassword()),
                user.getName(),
                user.getPhone(),
                user.getAddress()
        );

        ExternalUserEntity savedUser = externalUserRepository.save(newUser);

        log.info("고객 회원가입 완료 - email: {}", savedUser.getEmail());

        return ResExternalUserSignupDTO.from(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public ResExternalUserDTO getMyInfo(String email) {
        ExternalUserEntity user = externalUserRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        return ResExternalUserDTO.from(user);
    }

    @Override
    @Transactional
    public ResExternalUserDTO updateMyInfo(String email, ReqExternalUserUpdateDTO dto) {
        ExternalUserEntity user = externalUserRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        ReqExternalUserUpdateDTO.ExternalUser updateInfo = dto.getExternalUser();
        user.updateProfile(
                updateInfo.getName(),
                updateInfo.getPhone(),
                updateInfo.getAddress()
        );

        ExternalUserEntity updatedUser = externalUserRepository.save(user);

        log.info("고객 정보 수정 완료 - email: {}", email);

        return ResExternalUserDTO.from(updatedUser);
    }
}
