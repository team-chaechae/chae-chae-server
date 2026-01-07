package com.project.orderservice.infrastructure.config;

import com.project.orderservice.infrastructure.config.kafka.KafkaConsumerHelper;
import com.project.orderservice.infrastructure.config.kafka.KafkaConsumerMetricsListener;
import com.project.orderservice.infrastructure.config.kafka.KafkaProducerMetricsListener;
import com.project.orderservice.infrastructure.kafka.dto.InventoryConfirmedEvent;
import com.project.orderservice.infrastructure.kafka.dto.PaymentCompletedEvent;
import com.project.orderservice.infrastructure.kafka.dto.PaymentRefundedEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
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
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    // ==================== Producer 설정 ====================

    @Bean
    public ProducerFactory<String, Object> producerFactory(
            KafkaProducerMetricsListener<Object, Object> metricsListener) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        DefaultKafkaProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(config);
        addProducerMetricsListener(factory, metricsListener);
        return factory;
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(
            @Qualifier("producerFactory") ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    // Outbox 패턴용 String Producer - payload가 이미 JSON String이므로 StringSerializer 사용
    @Bean
    public ProducerFactory<String, String> stringProducerFactory(
            KafkaProducerMetricsListener<Object, Object> metricsListener) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 5);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 600000);
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 60000);
        config.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
        config.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 60000);
        config.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        DefaultKafkaProducerFactory<String, String> factory = new DefaultKafkaProducerFactory<>(config);
        addProducerMetricsListener(factory, metricsListener);
        return factory;
    }

    @Bean(name = {"stringKafkaTemplate", "dlqKafkaTemplate"})
    public KafkaTemplate<String, String> stringKafkaTemplate(
            @Qualifier("stringProducerFactory") ProducerFactory<String, String> stringProducerFactory) {
        return new KafkaTemplate<>(stringProducerFactory);
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
    public ConsumerFactory<String, PaymentCompletedEvent> paymentCompletedConsumerFactory(
            KafkaConsumerMetricsListener<Object, Object> metricsListener) {
        ConsumerFactory<String, PaymentCompletedEvent> factory = KafkaConsumerHelper.createConsumerFactory(
                bootstrapServers,
                "order-payment-group",
                PaymentCompletedEvent.class
        );
        if (factory instanceof DefaultKafkaConsumerFactory<String, PaymentCompletedEvent> defaultFactory) {
            addConsumerMetricsListener(defaultFactory, metricsListener);
        }
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentCompletedEvent> paymentCompletedListenerFactory(
            CommonErrorHandler kafkaErrorHandler,
            @Qualifier("paymentCompletedConsumerFactory")
            ConsumerFactory<String, PaymentCompletedEvent> paymentCompletedConsumerFactory) {
        return KafkaConsumerHelper.createListenerFactory(
                paymentCompletedConsumerFactory,
                kafkaErrorHandler,
                3
        );
    }

    // ==================== Payment Refunded Consumer 설정 ====================

    @Bean
    public ConsumerFactory<String, PaymentRefundedEvent> paymentRefundedConsumerFactory(
            KafkaConsumerMetricsListener<Object, Object> metricsListener) {
        ConsumerFactory<String, PaymentRefundedEvent> factory = KafkaConsumerHelper.createConsumerFactory(
                bootstrapServers,
                "order-payment-refund-group",
                PaymentRefundedEvent.class
        );
        if (factory instanceof DefaultKafkaConsumerFactory<String, PaymentRefundedEvent> defaultFactory) {
            addConsumerMetricsListener(defaultFactory, metricsListener);
        }
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentRefundedEvent> paymentRefundedListenerFactory(
            CommonErrorHandler kafkaErrorHandler,
            @Qualifier("paymentRefundedConsumerFactory")
            ConsumerFactory<String, PaymentRefundedEvent> paymentRefundedConsumerFactory) {
        return KafkaConsumerHelper.createListenerFactory(
                paymentRefundedConsumerFactory,
                kafkaErrorHandler,
                3
        );
    }

    // ==================== Inventory Confirmed Consumer 설정 ====================

    @Bean
    public ConsumerFactory<String, InventoryConfirmedEvent> inventoryConfirmedConsumerFactory(
            KafkaConsumerMetricsListener<Object, Object> metricsListener) {
        ConsumerFactory<String, InventoryConfirmedEvent> factory = KafkaConsumerHelper.createConsumerFactory(
                bootstrapServers,
                "order-inventory-confirmed-group",
                InventoryConfirmedEvent.class
        );
        if (factory instanceof DefaultKafkaConsumerFactory<String, InventoryConfirmedEvent> defaultFactory) {
            addConsumerMetricsListener(defaultFactory, metricsListener);
        }
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InventoryConfirmedEvent> inventoryConfirmedListenerFactory(
            CommonErrorHandler kafkaErrorHandler,
            @Qualifier("inventoryConfirmedConsumerFactory")
            ConsumerFactory<String, InventoryConfirmedEvent> inventoryConfirmedConsumerFactory) {
        return KafkaConsumerHelper.createListenerFactory(
                inventoryConfirmedConsumerFactory,
                kafkaErrorHandler,
                3
        );
    }

    @SuppressWarnings("unchecked")
    private static <K, V> void addProducerMetricsListener(
            DefaultKafkaProducerFactory<K, V> factory,
            KafkaProducerMetricsListener<Object, Object> metricsListener) {
        factory.addListener((ProducerFactory.Listener<K, V>) metricsListener);
    }

    @SuppressWarnings("unchecked")
    private static <K, V> void addConsumerMetricsListener(
            DefaultKafkaConsumerFactory<K, V> factory,
            KafkaConsumerMetricsListener<Object, Object> metricsListener) {
        factory.addListener((ConsumerFactory.Listener<K, V>) metricsListener);
    }
}
