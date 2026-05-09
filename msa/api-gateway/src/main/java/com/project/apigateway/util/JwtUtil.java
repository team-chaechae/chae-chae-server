package com.project.apigateway.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;

/**
 * JWT 검증 전용 유틸리티
 * 토큰 발급은 User-Service에서 담당
 */
@Component
@Slf4j
public class JwtUtil {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String AUTHORIZATION_KEY = "auth";
    public static final String BEARER_PREFIX = "Bearer ";

    @Value("${jwt.secret-key}")
    private String secretKey;

    private Key key;
    private JwtParser jwtParser;

    @PostConstruct
    public void init() {
        byte[] bytes = Base64.getDecoder().decode(secretKey);
        key = Keys.hmacShaKeyFor(bytes);
        jwtParser = Jwts.parserBuilder().setSigningKey(key).build();
    }

    /**
     * Bearer 토큰에서 JWT 추출
     */
    public String extractToken(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * JWT 토큰 검증
     */
    public boolean validateToken(String token) {
        try {
            jwtParser.parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("[JWT 검증] 유효하지 않은 JWT 서명입니다");
        } catch (ExpiredJwtException e) {
            log.error("[JWT 검증] 만료된 JWT 토큰입니다");
        } catch (UnsupportedJwtException e) {
            log.error("[JWT 검증] 지원하지 않는 JWT 토큰입니다");
        } catch (IllegalArgumentException e) {
            log.error("[JWT 검증] JWT 클레임이 비어있습니다");
        }
        return false;
    }

    /**
     * 토큰에서 Claims 추출
     */
    public Claims getClaims(String token) {
        try {
            return jwtParser.parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            log.warn("[JWT 검증] 만료된 JWT 토큰입니다");
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public String getSubject(String token) {
        return getClaims(token).getSubject();
    }

    public String getRole(String token) {
        return getClaims(token).get(AUTHORIZATION_KEY, String.class);
    }
}
