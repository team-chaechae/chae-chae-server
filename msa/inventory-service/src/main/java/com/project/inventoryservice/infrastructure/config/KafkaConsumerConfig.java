package com.project.inventoryservice.infrastructure.config;

import com.project.inventoryservice.infrastructure.config.kafka.KafkaConsumerHelper;
import com.project.inventoryservice.infrastructure.kafka.InventoryEvent;
import com.project.inventoryservice.infrastructure.kafka.dto.PaymentCompletedEvent;
import com.project.inventoryservice.infrastructure.kafka.dto.ProductCreatedEvent;
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
 * - payment-completed: 결제 완료 이벤트 (재고 차감)
 */
@EnableKafka
@Configuration
public class KafkaConsumerConfig {

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
    public ConcurrentKafkaListenerContainerFactory<String, InventoryEvent> kafkaListenerContainerFactory(
            CommonErrorHandler kafkaErrorHandler) {
        return KafkaConsumerHelper.createListenerFactory(
                consumerFactory(),
                kafkaErrorHandler,
                3
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InventoryEvent> batchKafkaListenerContainerFactory(
            CommonErrorHandler kafkaErrorHandler) {
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
    public ConcurrentKafkaListenerContainerFactory<String, ProductCreatedEvent> productCreatedListenerFactory(
            CommonErrorHandler kafkaErrorHandler) {
        return KafkaConsumerHelper.createListenerFactory(
                productCreatedConsumerFactory(),
                kafkaErrorHandler,
                1
        );
    }

    // ==================== Payment Completed Consumer ====================

    @Bean
    public ConsumerFactory<String, PaymentCompletedEvent> paymentCompletedConsumerFactory() {
        return KafkaConsumerHelper.createConsumerFactory(
                bootstrapServers,
                "inventory-payment-group",
                PaymentCompletedEvent.class
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentCompletedEvent> paymentCompletedListenerFactory(
            CommonErrorHandler kafkaErrorHandler) {
        return KafkaConsumerHelper.createListenerFactory(
                paymentCompletedConsumerFactory(),
                kafkaErrorHandler,
                3
        );
    }
}
