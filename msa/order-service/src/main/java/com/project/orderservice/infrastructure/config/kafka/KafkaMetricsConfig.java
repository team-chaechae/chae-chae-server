package com.project.orderservice.infrastructure.config.kafka;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaMetricsConfig {

    @Bean
    public KafkaProducerMetricsListener<Object, Object> kafkaProducerMetricsListener(
            MeterRegistry meterRegistry) {
        return new KafkaProducerMetricsListener<>(meterRegistry);
    }

    @Bean
    public KafkaConsumerMetricsListener<Object, Object> kafkaConsumerMetricsListener(
            MeterRegistry meterRegistry) {
        return new KafkaConsumerMetricsListener<>(meterRegistry);
    }
}
