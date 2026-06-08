package com.project.orderservice.infrastructure.config;

import com.project.orderservice.infrastructure.config.kafka.KafkaConsumerHelper;
import com.project.orderservice.infrastructure.kafka.dto.InventoryConfirmedEvent;
import com.project.orderservice.infrastructure.kafka.dto.PaymentCompletedEvent;
import com.project.orderservice.infrastructure.kafka.dto.PaymentRefundedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@EnableKafka
@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    private final CommonErrorHandler kafkaErrorHandler;

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    // ==================== Producer 설정 ====================

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // Outbox 패턴용 String Producer - payload가 이미 JSON String이므로 StringSerializer 사용
    @Bean
    public ProducerFactory<String, String> stringProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, String> stringKafkaTemplate() {
        return new KafkaTemplate<>(stringProducerFactory());
    }

    @Bean
    public KafkaTemplate<String, String> dlqKafkaTemplate() {
        return new KafkaTemplate<>(stringProducerFactory());
    }

    @Bean
    public NewTopic paymentCompletedTopic() {
        return TopicBuilder.name("payment-completed")
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000")  // 7일 보관
                .build();
    }

    @Bean
    public NewTopic orderCreatedTopic() {
        return TopicBuilder.name("order-created")
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000")  // 7일 보관
                .build();
    }

    // ==================== Consumer 설정 (ErrorHandlingDeserializer 적용) ====================

    @Bean
    public ConsumerFactory<String, PaymentCompletedEvent> paymentCompletedConsumerFactory() {
        return KafkaConsumerHelper.createConsumerFactory(
                bootstrapServers,
                "order-payment-group",
                PaymentCompletedEvent.class
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentCompletedEvent> paymentCompletedListenerFactory() {
        return KafkaConsumerHelper.createListenerFactory(
                paymentCompletedConsumerFactory(),
                kafkaErrorHandler,
                3
        );
    }

    // ==================== Payment Refunded Consumer 설정 ====================

    @Bean
    public ConsumerFactory<String, PaymentRefundedEvent> paymentRefundedConsumerFactory() {
        return KafkaConsumerHelper.createConsumerFactory(
                bootstrapServers,
                "order-payment-refund-group",
                PaymentRefundedEvent.class
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentRefundedEvent> paymentRefundedListenerFactory() {
        return KafkaConsumerHelper.createListenerFactory(
                paymentRefundedConsumerFactory(),
                kafkaErrorHandler,
                3
        );
    }

    // ==================== Inventory Confirmed Consumer 설정 ====================

    @Bean
    public ConsumerFactory<String, InventoryConfirmedEvent> inventoryConfirmedConsumerFactory() {
        return KafkaConsumerHelper.createConsumerFactory(
                bootstrapServers,
                "order-inventory-confirmed-group",
                InventoryConfirmedEvent.class
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InventoryConfirmedEvent> inventoryConfirmedListenerFactory() {
        return KafkaConsumerHelper.createListenerFactory(
                inventoryConfirmedConsumerFactory(),
                kafkaErrorHandler,
                3
        );
    }
}
