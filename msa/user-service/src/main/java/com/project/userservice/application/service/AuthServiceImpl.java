package com.project.userservice.application.service;

import com.project.userservice.application.global.exception.BadRequestException;
import com.project.userservice.application.response.ResAuthLoginDTO;
import com.project.userservice.domain.model.ExternalUserEntity;
import com.project.userservice.domain.model.InternalUserEntity;
import com.project.userservice.domain.repository.ExternalUserRepository;
import com.project.userservice.domain.repository.InternalUserRepository;
import com.project.userservice.infrastructure.util.JwtUtil;
import com.project.userservice.presentation.request.ReqAuthPostLoginDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final InternalUserRepository internalUserRepository;
    private final ExternalUserRepository externalUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    public ResAuthLoginDTO login(ReqAuthPostLoginDTO request) {
        String email = request.getUser().getEmail();
        String password = request.getUser().getPassword();

        // 1. 내부 사용자(직원) 먼저 조회
        Optional<InternalUserEntity> internalUser = internalUserRepository.findByEmail(email);
        if (internalUser.isPresent()) {
            return loginAsInternalUser(internalUser.get(), password);
        }

        // 2. 외부 사용자(고객) 조회
        Optional<ExternalUserEntity> externalUser = externalUserRepository.findByEmail(email);
        if (externalUser.isPresent()) {
            return loginAsExternalUser(externalUser.get(), password);
        }

        throw new BadRequestException("이메일 또는 비밀번호가 올바르지 않습니다.");
    }

    private ResAuthLoginDTO loginAsInternalUser(InternalUserEntity user, String password) {
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadRequestException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        log.info("내부 사용자 '{}' 로그인 성공 (role: {})", user.getEmail(), user.getRole());

        return ResAuthLoginDTO.of(accessToken, refreshToken, user.getEmail(), user.getRole(), user.getRealName());
    }

    private ResAuthLoginDTO loginAsExternalUser(ExternalUserEntity user, String password) {
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadRequestException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        log.info("고객 '{}' 로그인 성공 (role: {})", user.getEmail(), user.getRole());

        return ResAuthLoginDTO.of(accessToken, refreshToken, user.getEmail(), user.getRole(), user.getName());
    }

    @Override
    public void logout(String token) {
        // Bearer 제거
        String pureToken = jwtUtil.extractToken(token);
        if (pureToken == null) {
            throw new BadRequestException("유효하지 않은 토큰입니다.");
        }

        // 토큰 만료 시간까지 블랙리스트에 추가
        long expiration = jwtUtil.getExpiration(pureToken);
        tokenBlacklistService.addToBlacklist(pureToken, expiration);

        String email = jwtUtil.getSubject(pureToken);
        log.info("사용자 '{}' 로그아웃 완료, 토큰 블랙리스트 추가", email);
    }

    @Override
    public ResAuthLoginDTO refresh(String refreshToken) {
        // Refresh Token 검증
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BadRequestException("유효하지 않거나 만료된 리프레시 토큰입니다.");
        }

        // 블랙리스트 확인
        if (tokenBlacklistService.isBlacklisted(refreshToken)) {
            throw new BadRequestException("이미 로그아웃된 토큰입니다.");
        }

        String email = jwtUtil.getSubject(refreshToken);

        // 1. 내부 사용자(직원) 먼저 조회
        Optional<InternalUserEntity> internalUser = internalUserRepository.findByEmail(email);
        if (internalUser.isPresent()) {
            InternalUserEntity user = internalUser.get();
            String newAccessToken = jwtUtil.generateAccessToken(email, user.getRole().name());
            log.info("내부 사용자 '{}' 토큰 갱신 완료", email);
            return ResAuthLoginDTO.of(newAccessToken, refreshToken, email, user.getRole(), user.getRealName());
        }

        // 2. 외부 사용자(고객) 조회
        Optional<ExternalUserEntity> externalUser = externalUserRepository.findByEmail(email);
        if (externalUser.isPresent()) {
            ExternalUserEntity user = externalUser.get();
            String newAccessToken = jwtUtil.generateAccessToken(email, user.getRole().name());
            log.info("고객 '{}' 토큰 갱신 완료", email);
            return ResAuthLoginDTO.of(newAccessToken, refreshToken, email, user.getRole(), user.getName());
        }

        throw new BadRequestException("사용자를 찾을 수 없습니다.");
    }
}
