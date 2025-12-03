package com.project.userservice.infrastructure.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
@Slf4j
public class JwtUtil {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String AUTHORIZATION_KEY = "auth";
    public static final String BEARER_PREFIX = "Bearer ";

    private static final long ACCESS_TOKEN_TIME = 60 * 60 * 1000L; // 1시간
    private static final long REFRESH_TOKEN_TIME = 14 * 24 * 60 * 60 * 1000L; // 2주

    private final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

    @Value("${jwt.secret-key}")
    private String secretKey;

    private Key key;

    @PostConstruct
    public void init() {
        byte[] bytes = Base64.getDecoder().decode(secretKey);
        key = Keys.hmacShaKeyFor(bytes);
    }

    /**
     * Access Token 발급
     */
    public String generateAccessToken(String email, String role) {
        Date now = new Date();

        return BEARER_PREFIX + Jwts.builder()
                .setSubject(email)
                .claim(AUTHORIZATION_KEY, role)
                .setExpiration(new Date(now.getTime() + ACCESS_TOKEN_TIME))
                .setIssuedAt(now)
                .signWith(key, signatureAlgorithm)
                .compact();
    }

    /**
     * Refresh Token 발급
     */
    public String generateRefreshToken(String email) {
        Date now = new Date();

        return Jwts.builder()
                .setSubject(email)
                .setExpiration(new Date(now.getTime() + REFRESH_TOKEN_TIME))
                .setIssuedAt(now)
                .signWith(key, signatureAlgorithm)
                .compact();
    }

    /**
     * 토큰에서 Bearer 제거
     */
    public String extractToken(String bearerToken) {
        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 토큰 검증
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
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
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    public String getSubject(String token) {
        return getClaims(token).getSubject();
    }

    public String getRole(String token) {
        return getClaims(token).get(AUTHORIZATION_KEY, String.class);
    }

    public long getAccessTokenTime() {
        return ACCESS_TOKEN_TIME / 1000; // 초 단위
    }

    public long getRefreshTokenTime() {
        return REFRESH_TOKEN_TIME / 1000; // 초 단위
    }

    /**
     * 토큰 만료 시간 추출 (블랙리스트 TTL용)
     */
    public long getExpiration(String token) {
        Claims claims = getClaims(token);
        Date expiration = claims.getExpiration();
        return (expiration.getTime() - System.currentTimeMillis()) / 1000;
    }
}
