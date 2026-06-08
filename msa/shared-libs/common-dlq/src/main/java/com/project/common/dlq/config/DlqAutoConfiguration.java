package com.project.common.dlq.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.dlq.alert.DlqAlertService;
import com.project.common.dlq.alert.SlackWebhookClient;
import com.project.common.dlq.domain.DlqRecord;
import com.project.common.dlq.domain.DlqRecordRepository;
import com.project.common.dlq.exception.DlqExceptionClassifier;
import com.project.common.dlq.handler.DlqErrorHandler;
import com.project.common.dlq.handler.DlqRecoverer;
import com.project.common.dlq.producer.DlqMessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.HashMap;
import java.util.Map;

/**
 * DLQ 자동 설정
 *
 * DLQ 자동 설정
 */
@Slf4j
@AutoConfiguration(before = {
        HibernateJpaAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class
})
@EnableScheduling
@EnableConfigurationProperties(DlqProperties.class)
@AutoConfigurationPackage(basePackageClasses = DlqRecord.class)
@ConditionalOnProperty(prefix = "dlq", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DlqAutoConfiguration {

    private final DlqProperties properties;

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.application.name:unknown-service}")
    private String serviceName;

    /**
     * 예외 분류기
     */
    @Bean
    @ConditionalOnMissingBean
    public DlqExceptionClassifier dlqExceptionClassifier() {
        return new DlqExceptionClassifier();
    }

    /**
     * DLQ 메시지 Producer
     */
    @Bean
    @ConditionalOnMissingBean
    public DlqMessageProducer dlqMessageProducer(
            @Qualifier("dlqKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        return new DlqMessageProducer(kafkaTemplate, objectMapper, properties);
    }

    /**
     * DLQ Slack 웹훅 클라이언트
     */
    @Bean
    @ConditionalOnMissingBean
    public SlackWebhookClient slackWebhookClient(
            @Value("${slack.webhook.url:}") String webhookUrl,
            @Value("${spring.application.name:unknown-service}") String serviceName,
            @Value("${slack.alert.enabled:false}") boolean enabled) {
        return new SlackWebhookClient(webhookUrl, serviceName, enabled);
    }

    /**
     * DLQ 알림 서비스
     */
    @Bean
    @ConditionalOnMissingBean
    public DlqAlertService dlqAlertService(
            SlackWebhookClient slackWebhookClient,
            DlqRecordRepository dlqRecordRepository) {
        return new DlqAlertService(slackWebhookClient, dlqRecordRepository, properties);
    }

    /**
     * DLQ Recoverer
     */
    @Bean
    @ConditionalOnMissingBean
    public DlqRecoverer dlqRecoverer(
            DlqMessageProducer dlqProducer,
            DlqAlertService alertService,
            DlqExceptionClassifier classifier,
            DlqRecordRepository dlqRecordRepository) {
        return new DlqRecoverer(
                dlqProducer,
                alertService,
                classifier,
                dlqRecordRepository,
                properties,
                serviceName
        );
    }

    /**
     * DLQ Error Handler (기존 ErrorHandler 대체용)
     */
    @Bean
    @ConditionalOnMissingBean(DlqErrorHandler.class)
    public DlqErrorHandler dlqErrorHandler(
            DlqRecoverer recoverer,
            DlqExceptionClassifier classifier) {
        log.info("[DLQ AutoConfig] DlqErrorHandler 생성 - service: {}", serviceName);
        return new DlqErrorHandler(recoverer, classifier, properties);
    }

    /**
     * DLQ 리플레이용 Consumer Factory
     */
    @Bean
    public ConsumerFactory<String, String> dlqReplayConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, properties.getTechnicalReplay().getBatchSize());

        return new DefaultKafkaConsumerFactory<>(config);
    }

    /**
     * DLQ 리플레이용 Listener Container Factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> dlqReplayListenerFactory(
            ConsumerFactory<String, String> dlqReplayConsumerFactory) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(dlqReplayConsumerFactory);
        factory.setConcurrency(1);  // DLQ 리플레이는 순서대로 처리
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        log.info("[DLQ AutoConfig] DLQ 리플레이 Listener Factory 생성 완료");
        return factory;
    }
}
