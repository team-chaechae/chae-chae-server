package com.project.inventoryservice;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * 테스트용 KafkaTemplate 빈 제공.
 */
@Configuration
@Profile("test")
public class TestKafkaConfig {

    @Bean
    public KafkaTemplate<String, String> dlqKafkaTemplate() {
        return Mockito.mock(KafkaTemplate.class);
    }
}
