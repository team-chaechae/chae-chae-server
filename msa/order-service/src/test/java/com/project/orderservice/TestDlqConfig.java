package com.project.orderservice;

import com.project.common.dlq.handler.DlqErrorHandler;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 테스트용 DLQ 핸들러 빈 제공.
 */
@Configuration
@Profile("test")
public class TestDlqConfig {

    @Bean
    public DlqErrorHandler dlqErrorHandler() {
        return Mockito.mock(DlqErrorHandler.class);
    }
}
