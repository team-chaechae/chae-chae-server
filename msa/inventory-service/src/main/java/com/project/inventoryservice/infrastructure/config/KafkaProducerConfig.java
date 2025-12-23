package com.project.inventoryservice.infrastructure.config;

import com.project.inventoryservice.infrastructure.kafka.InventoryEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Producer 설정
 * inventory-events 토픽으로 재고 변경 이벤트 발행 (DB 동기화용)
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    private Map<String, Object> commonProducerConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 32768);
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
        return config;
    }

    @Bean
    public ProducerFactory<String, InventoryEvent> producerFactory() {
        Map<String, Object> config = commonProducerConfig();
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, InventoryEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // Generic Object Producer (for various event types)
    @Bean
    public ProducerFactory<String, Object> objectProducerFactory() {
        Map<String, Object> config = commonProducerConfig();
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> objectKafkaTemplate() {
        return new KafkaTemplate<>(objectProducerFactory());
    }

    // ==================== Topic 설정 ====================

    @Bean
    public NewTopic inventoryConfirmedTopic() {
        return TopicBuilder.name("inventory-confirmed")
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000")  // 7일 보관
                .build();
    }
}
