package com.project.userservice.application.service;

import com.project.userservice.application.global.exception.BadRequestException;
import com.project.userservice.application.response.ResAuthLoginDTO;
import com.project.userservice.domain.model.InternalUserEntity;
import com.project.userservice.domain.repository.InternalUserRepository;
import com.project.userservice.infrastructure.util.JwtUtil;
import com.project.userservice.presentation.request.ReqAuthPostLoginDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final InternalUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    public ResAuthLoginDTO login(ReqAuthPostLoginDTO request) {
        String email = request.getUser().getEmail();
        String password = request.getUser().getPassword();

        // 사용자 조회
        InternalUserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("이메일 또는 비밀번호가 올바르지 않습니다."));

        // 비밀번호 검증
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadRequestException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // JWT 토큰 발급
        String accessToken = jwtUtil.generateAccessToken(email, user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(email);

        log.info("사용자 '{}' 로그인 성공, JWT 발급 완료", email);

        return ResAuthLoginDTO.of(accessToken, refreshToken, email, user.getRole(), user.getRealName());
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

        // 사용자 조회
        InternalUserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("사용자를 찾을 수 없습니다."));

        // 새로운 Access Token 발급
        String newAccessToken = jwtUtil.generateAccessToken(email, user.getRole().name());

        log.info("사용자 '{}' 토큰 갱신 완료", email);

        return ResAuthLoginDTO.of(newAccessToken, refreshToken, email, user.getRole(), user.getRealName());
    }
}
