package com.project.chaechaeserver.presentation.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.chaechaeserver.application.service.user.RedisRefreshTokenService;
import com.project.chaechaeserver.domain.model.user.constraint.RoleType;
import com.project.chaechaeserver.infrastructure.security.CustomUserDetails;
import com.project.chaechaeserver.infrastructure.util.JwtUtil;
import com.project.chaechaeserver.presentation.request.user.ReqAuthPostLoginDTO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Slf4j(topic = "로그인 및 JWT 생성")
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final JwtUtil jwtUtil;
    private final RedisRefreshTokenService redisRefreshTokenService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, RedisRefreshTokenService redisRefreshTokenService) {
        this.jwtUtil = jwtUtil;
        this.redisRefreshTokenService = redisRefreshTokenService;
        setFilterProcessesUrl("/api/users/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        try {
            ReqAuthPostLoginDTO dto = new ObjectMapper().readValue(request.getInputStream(), ReqAuthPostLoginDTO.class);

            return getAuthenticationManager().authenticate(
                    new UsernamePasswordAuthenticationToken(
                            dto.getUser().getEmail(),
                            dto.getUser().getPassword(),
                            null
                    )
            );
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) {
        String email = ((CustomUserDetails) authResult.getPrincipal()).getUsername();
        RoleType role = ((CustomUserDetails) authResult.getPrincipal()).getUser().getRole();

        // Redis 에 Refresh Token 저장
        if (redisRefreshTokenService.getRefreshToken(email) == null) {
            String refreshJwt = jwtUtil.generateRefreshJwt();
            redisRefreshTokenService.saveRefreshToken(email, refreshJwt);
            response.addCookie(jwtUtil.generateRefreshJwtCookie(refreshJwt));

            log.info("사용자 '{}' Refresh Token 발급 완료", email);
        }

        // Access Token 헤더에 담기
        String token = jwtUtil.generateAccessJwt(email, role);

        response.addHeader(JwtUtil.ACCESS_JWT_HEADER, token);

        log.info("사용자 '{}' 로그인 성공, Access Token 발급 완료", email);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) {
        response.setStatus(401);
    }

}