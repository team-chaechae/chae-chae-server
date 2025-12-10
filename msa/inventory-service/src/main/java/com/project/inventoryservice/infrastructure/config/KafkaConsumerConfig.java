package com.project.inventoryservice.infrastructure.config;

import com.project.inventoryservice.infrastructure.kafka.InventoryEvent;
import com.project.inventoryservice.infrastructure.kafka.dto.InventoryReserveEvent;
import com.project.inventoryservice.infrastructure.kafka.dto.ProductCreatedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Consumer 설정
 */
@EnableKafka
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public ConsumerFactory<String, InventoryEvent> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "inventory-consumer-group");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        // JSON Deserializer 설정
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.project.inventoryservice.infrastructure.kafka");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, InventoryEvent.class.getName());

        // 자동 커밋 비활성화 (수동 커밋으로 at-least-once 보장)
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InventoryEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, InventoryEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);  // 파티션 수와 맞춤

        // 수동 ACK 모드
        factory.getContainerProperties().setAckMode(
            org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        return factory;
    }

    /**
     * 배치 리스너 팩토리 (inventory-events 배치 처리용)
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InventoryEvent> batchKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, InventoryEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);  // 파티션 수와 맞춤

        // 배치 리스너 활성화
        factory.setBatchListener(true);

        // 수동 ACK 모드
        factory.getContainerProperties().setAckMode(
            org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        return factory;
    }

    // Saga - 재고 차감 요청 Consumer
    @Bean
    public ConsumerFactory<String, InventoryReserveEvent> inventoryReserveConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "inventory-saga-group");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        JsonDeserializer<InventoryReserveEvent> deserializer = new JsonDeserializer<>(InventoryReserveEvent.class);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeMapperForKey(true);

        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InventoryReserveEvent> inventoryReserveListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, InventoryReserveEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(inventoryReserveConsumerFactory());
        factory.setConcurrency(3);
        return factory;
    }

    // Product Created 이벤트 Consumer
    @Bean
    public ConsumerFactory<String, ProductCreatedEvent> productCreatedConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "inventory-product-group");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        JsonDeserializer<ProductCreatedEvent> deserializer = new JsonDeserializer<>(ProductCreatedEvent.class);
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeMapperForKey(true);

        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ProductCreatedEvent> productCreatedListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ProductCreatedEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(productCreatedConsumerFactory());
        factory.setConcurrency(1);  // 상품 생성은 빈도가 낮으므로 1개로 충분
        return factory;
    }
}
