package com.project.deliveryservice.infrastructure.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@EnableKafka
@Configuration
public class KafkaConfig {

    private static final String DELIVERY_CREATE_REQUESTED_TOPIC = "delivery-create-requested";
    private static final String DELIVERY_CANCEL_REQUESTED_TOPIC = "delivery-cancel-requested";
    private static final String DELIVERY_STATUS_CHANGED_TOPIC = "delivery-status-changed";
    private static final String DLQ_TOPIC_SUFFIX = ".dlq";

    @Value("${spring.kafka.bootstrap-servers:localhost:9093}")
    private String bootstrapServers;

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
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> stringKafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                stringKafkaTemplate,
                (record, exception) -> new TopicPartition(record.topic() + DLQ_TOPIC_SUFFIX, record.partition())
        );
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2));
    }

    @Bean
    public NewTopic deliveryCreateRequestedTopic() {
        return TopicBuilder.name(DELIVERY_CREATE_REQUESTED_TOPIC)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000")
                .build();
    }

    @Bean
    public NewTopic deliveryCreateRequestedDlqTopic() {
        return TopicBuilder.name(DELIVERY_CREATE_REQUESTED_TOPIC + DLQ_TOPIC_SUFFIX)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000")
                .build();
    }

    @Bean
    public NewTopic deliveryStatusChangedTopic() {
        return TopicBuilder.name(DELIVERY_STATUS_CHANGED_TOPIC)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000")
                .build();
    }

    @Bean
    public NewTopic deliveryStatusChangedDlqTopic() {
        return TopicBuilder.name(DELIVERY_STATUS_CHANGED_TOPIC + DLQ_TOPIC_SUFFIX)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000")
                .build();
    }

    @Bean
    public NewTopic deliveryCancelRequestedTopic() {
        return TopicBuilder.name(DELIVERY_CANCEL_REQUESTED_TOPIC)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000")
                .build();
    }

    @Bean
    public NewTopic deliveryCancelRequestedDlqTopic() {
        return TopicBuilder.name(DELIVERY_CANCEL_REQUESTED_TOPIC + DLQ_TOPIC_SUFFIX)
                .partitions(3)
                .replicas(1)
                .config("retention.ms", "604800000")
                .build();
    }
}
