package com.project.apigateway.security;

import com.project.apigateway.util.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String RAW_SECRET = "0123456789abcdef0123456789abcdef";
    private static final String BASE64_SECRET = Base64.getEncoder()
            .encodeToString(RAW_SECRET.getBytes(StandardCharsets.UTF_8));

    @Mock
    private ReactiveRedisTemplate<String, String> redisTemplate;

    private JwtAuthenticationFilter filter;
    private Key key;

    @BeforeEach
    void setUp() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", BASE64_SECRET);
        jwtUtil.init();

        filter = new JwtAuthenticationFilter(jwtUtil, redisTemplate);
        key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(BASE64_SECRET));
    }

    @Test
    @DisplayName("만료된 JWT는 SecurityContext에 인증 정보를 만들면 안 된다")
    void expiredJwtDoesNotAuthenticateRequest() {
        String expiredToken = Jwts.builder()
                .setSubject("customer@example.com")
                .claim(JwtUtil.AUTHORIZATION_KEY, "CUSTOMER")
                .setIssuedAt(new Date(System.currentTimeMillis() - 7_200_000))
                .setExpiration(new Date(System.currentTimeMillis() - 3_600_000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        lenient().when(redisTemplate.hasKey("blacklist:" + expiredToken)).thenReturn(Mono.just(false));

        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/sales")
                        .header(HttpHeaders.AUTHORIZATION, JwtUtil.BEARER_PREFIX + expiredToken)
        );

        WebFilterChain chain = chainedExchange -> ReactiveSecurityContextHolder.getContext()
                .flatMap(context -> Mono.<Void>error(new AssertionError("expired token was authenticated")))
                .switchIfEmpty(Mono.<Void>empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
    }
}
