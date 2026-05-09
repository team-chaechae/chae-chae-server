package com.project.orderservice.infrastructure.config;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        // TODO: 인증 구현 시 SecurityContext에서 사용자 정보 가져오기
        return () -> Optional.of("SYSTEM");
    }
}
