package com.project.chaechaeserver.presentation.filter;

import com.project.chaechaeserver.application.service.user.RedisRefreshTokenService;
import com.project.chaechaeserver.domain.model.user.constraint.RoleType;
import com.project.chaechaeserver.infrastructure.security.CustomUserDetailsService;
import com.project.chaechaeserver.infrastructure.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

@Slf4j(topic = "JWT 검증 및 인가")
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;
    private final RedisRefreshTokenService redisRefreshTokenService;

    public JwtAuthorizationFilter(JwtUtil jwtUtil, CustomUserDetailsService customUserDetailsService, RedisRefreshTokenService redisRefreshTokenService) {
        this.jwtUtil = jwtUtil;
        this.customUserDetailsService = customUserDetailsService;
        this.redisRefreshTokenService = redisRefreshTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain filterChain) throws ServletException, IOException {

        String tokenValue = jwtUtil.getJwtFromHeader(req);

        if (StringUtils.hasText(tokenValue)) {

            if (!jwtUtil.validateToken(tokenValue)) {
                log.error("Token Error");
                refreshAccessJwt(req, res);
                return;
            }

            Claims info = jwtUtil.getUserInfoFromToken(tokenValue);

            try {
                setAuthentication(info.getSubject());
            } catch (Exception e) {
                log.error(e.getMessage());
                return;
            }
        }

        filterChain.doFilter(req, res);
    }

    public void setAuthentication(String username) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        Authentication authentication = createAuthentication(username);
        context.setAuthentication(authentication);

        SecurityContextHolder.setContext(context);
    }

    private Authentication createAuthentication(String username) {
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    public void refreshAccessJwt(HttpServletRequest req, HttpServletResponse res) throws IOException {

        String expiredAccessJwt = jwtUtil.getJwtFromHeader(req);
        Claims claims = jwtUtil.getUserInfoFromToken(expiredAccessJwt);
        String email = claims.getSubject();
        String role = claims.get(JwtUtil.AUTHORIZATION_KEY, String.class);

        // Refresh Token 추출
        String refreshJwtFromCookie = "";

        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (JwtUtil.REFRESH_JWT_HEADER.equals(cookie.getName())) {
                    refreshJwtFromCookie = cookie.getValue();
                    break;
                }
            }
        }

        log.debug("Refresh Token 추출 완료");

        String refreshJwtFromRedis = redisRefreshTokenService.getRefreshToken(email);

        // Refresh Token 유효성 검증
        if (!StringUtils.hasText(refreshJwtFromCookie) || !jwtUtil.validateToken(refreshJwtFromCookie) || !Objects.equals(refreshJwtFromCookie, refreshJwtFromRedis)) {
            log.info("사용자 '{}'의 Refresh Token 만료 또는 유효하지 않음", email);
            redisRefreshTokenService.deleteRefreshToken(email);
            res.sendError(401, "Refresh Token이 존재하지 않거나 만료됐습니다.");
            return;
        }

        // 새로운 AccessToken 발급
        String newAccessJwt = jwtUtil.generateAccessJwt(email, RoleType.valueOf(role));

        // 헤더를 통해 전달
        res.addHeader(JwtUtil.ACCESS_JWT_HEADER, newAccessJwt);

        log.info("사용자 '{}'의 새로운 Access Token 발급 완료", email);
    }
}