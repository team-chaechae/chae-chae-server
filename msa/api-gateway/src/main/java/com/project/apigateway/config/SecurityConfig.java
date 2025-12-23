package com.project.apigateway.config;

import com.project.apigateway.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)

                // 인증 실패 시 401 반환 (기본 로그인 페이지 비활성화)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(unauthorizedEntryPoint())
                )

                // JWT 필터 추가
                .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)

                // 경로별 권한 설정
                .authorizeExchange(exchanges -> exchanges
                        // 인증 없이 접근 가능
                        .pathMatchers("/api/users/auth/**").permitAll()
                        .pathMatchers("/api/users/signup").permitAll()
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // ADMIN 역할만 접근 가능
                        .pathMatchers("/api/admin/**").hasRole("ADMIN")
                        .pathMatchers(HttpMethod.DELETE).hasRole("ADMIN")

                        // EMPLOYEE, ADMIN 역할 접근 가능 (재고, 상품, 주문 관리)
                        .pathMatchers("/api/inventory/**").hasAnyRole("EMPLOYEE", "ADMIN")
                        .pathMatchers("/api/products/**").hasAnyRole("EMPLOYEE", "ADMIN")
                        .pathMatchers("/api/orders/**").hasAnyRole("EMPLOYEE", "ADMIN")

                        // CUSTOMER, EMPLOYEE, ADMIN 모두 접근 가능
                        .pathMatchers("/api/chatbot/**").hasAnyRole("CUSTOMER", "EMPLOYEE", "ADMIN")

                        // 그 외 인증된 사용자
                        .anyExchange().authenticated()
                )
                .build();
    }

    private ServerAuthenticationEntryPoint unauthorizedEntryPoint() {
        return (exchange, ex) -> {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().add("Content-Type", "application/json");
            String body = "{\"code\":401,\"message\":\"인증이 필요합니다\"}";
            return exchange.getResponse().writeWith(
                    Mono.just(exchange.getResponse().bufferFactory().wrap(body.getBytes()))
            );
        };
    }
}
