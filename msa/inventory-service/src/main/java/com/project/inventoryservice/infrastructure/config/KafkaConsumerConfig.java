package com.project.inventoryservice.infrastructure.config;

import com.project.inventoryservice.infrastructure.config.kafka.KafkaConsumerHelper;
import com.project.inventoryservice.infrastructure.kafka.InventoryEvent;
import com.project.inventoryservice.infrastructure.kafka.dto.ProductCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;

/**
 * Kafka Consumer 설정
 * - inventory-events: 재고 변경 이벤트 (DB 동기화용)
 * - product-created: 상품 생성 이벤트
 */
@EnableKafka
@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    private final CommonErrorHandler kafkaErrorHandler;

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    // ==================== Inventory Event Consumer (ErrorHandlingDeserializer 적용) ====================

    @Bean
    public ConsumerFactory<String, InventoryEvent> consumerFactory() {
        return KafkaConsumerHelper.createConsumerFactory(
                bootstrapServers,
                "inventory-consumer-group",
                InventoryEvent.class
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InventoryEvent> kafkaListenerContainerFactory() {
        return KafkaConsumerHelper.createListenerFactory(
                consumerFactory(),
                kafkaErrorHandler,
                3
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InventoryEvent> batchKafkaListenerContainerFactory() {
        return KafkaConsumerHelper.createBatchListenerFactory(
                consumerFactory(),
                kafkaErrorHandler,
                3
        );
    }

    // ==================== Product Created Consumer ====================

    @Bean
    public ConsumerFactory<String, ProductCreatedEvent> productCreatedConsumerFactory() {
        return KafkaConsumerHelper.createConsumerFactory(
                bootstrapServers,
                "inventory-product-group",
                ProductCreatedEvent.class
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ProductCreatedEvent> productCreatedListenerFactory() {
        return KafkaConsumerHelper.createListenerFactory(
                productCreatedConsumerFactory(),
                kafkaErrorHandler,
                1
        );
    }

}
