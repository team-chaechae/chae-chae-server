package com.project.paymentservice.infrastructure.config;

import com.project.paymentservice.infrastructure.config.kafka.KafkaConsumerHelper;
import com.project.paymentservice.infrastructure.kafka.dto.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
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

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    public static final String TOPIC_PAYMENT_COMPLETED = "payment-completed";
    public static final String TOPIC_PAYMENT_REFUNDED = "payment-refunded";

    private final CommonErrorHandler kafkaErrorHandler;

    @Value("${spring.kafka.bootstrap-servers:localhost:9093}")
    private String bootstrapServers;

    // ==================== Producer 설정 ====================

    @Bean
    public ProducerFactory<String, String> producerFactory() {
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
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public NewTopic paymentCompletedTopic() {
        return TopicBuilder.name(TOPIC_PAYMENT_COMPLETED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentRefundedTopic() {
        return TopicBuilder.name(TOPIC_PAYMENT_REFUNDED)
                .partitions(3)
                .replicas(1)
                .build();
    }

    // ==================== Consumer 설정 (ErrorHandlingDeserializer 적용) ====================

    @Bean
    public ConsumerFactory<String, OrderCreatedEvent> orderCreatedConsumerFactory() {
        return KafkaConsumerHelper.createConsumerFactory(
                bootstrapServers,
                "payment-order-group",
                OrderCreatedEvent.class
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCreatedEvent> orderCreatedListenerFactory() {
        return KafkaConsumerHelper.createListenerFactory(
                orderCreatedConsumerFactory(),
                kafkaErrorHandler,
                3
        );
    }

}
