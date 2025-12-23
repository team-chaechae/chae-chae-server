package com.project.apigateway.security;

import com.project.apigateway.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtUtil jwtUtil;
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    private static final String BLACKLIST_PREFIX = "blacklist:";

    public JwtAuthenticationFilter(JwtUtil jwtUtil,
                                   @Qualifier("reactiveStringRedisTemplate") ReactiveRedisTemplate<String, String> redisTemplate) {
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String token = jwtUtil.extractToken(authHeader);

        log.info("[Gateway] 요청 수신: {} {} ", method, path);

        // 토큰이 없으면 다음 필터로 (permitAll 경로는 SecurityConfig에서 처리)
        if (token == null) {
            log.warn("[Gateway] 인증 헤더 없음: {} {}", method, path);
            return chain.filter(exchange);
        }

        // 토큰 검증 및 Claims 추출 (한 번만 파싱)
        var claims = jwtUtil.getClaims(token);
        if (claims == null) {
            log.warn("[Gateway] 유효하지 않은 토큰: {} {}", method, path);
            return chain.filter(exchange);
        }

        // Claims에서 정보 추출 (추가 파싱 없음)
        String email = claims.getSubject();
        String role = claims.get(JwtUtil.AUTHORIZATION_KEY, String.class);

        // 블랙리스트 확인 후 인증 처리
        return isBlacklisted(token)
                .flatMap(isBlacklisted -> {
                    if (isBlacklisted) {
                        log.warn("블랙리스트에 등록된 토큰 사용 시도");
                        return chain.filter(exchange);
                    }

                    log.info("[Gateway] 인증 성공: user={}, role={}, path={}", email, role, path);

                    // Spring Security Authority 생성 (ROLE_ 접두사 필요)
                    List<SimpleGrantedAuthority> authorities = List.of(
                            new SimpleGrantedAuthority("ROLE_" + role)
                    );

                    // Authentication 객체 생성
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(email, null, authorities);

                    // 사용자 정보 헤더 추가 (다운스트림 서비스용)
                    ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                            .header("User-Id", email)
                            .header("User-Role", role)
                            .build();

                    ServerWebExchange modifiedExchange = exchange.mutate()
                            .request(modifiedRequest)
                            .build();

                    // SecurityContext에 인증 정보 설정
                    return chain.filter(modifiedExchange)
                            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
                });
    }

    private Mono<Boolean> isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return redisTemplate.hasKey(key);
    }
}
