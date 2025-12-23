package com.project.paymentservice.infrastructure.config.kafka;

import com.project.paymentservice.infrastructure.alert.SlackAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaErrorHandlerConfig {

    private final SlackAlertService slackAlertService;

    @Bean
    public CommonErrorHandler kafkaErrorHandler() {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                (consumerRecord, exception) -> {
                    String topic = consumerRecord != null ? consumerRecord.topic() : "unknown";
                    String key = consumerRecord != null && consumerRecord.key() != null
                            ? consumerRecord.key().toString() : "unknown";

                    log.error("[Kafka Error Handler] 메시지 처리 실패 - topic: {}, key: {}, partition: {}, offset: {}, error: {}",
                            topic, key,
                            consumerRecord != null ? consumerRecord.partition() : -1,
                            consumerRecord != null ? consumerRecord.offset() : -1,
                            exception.getMessage(), exception);

                    slackAlertService.sendKafkaErrorAlert(
                            topic,
                            String.format("메시지 처리 실패 - key: %s, partition: %d, offset: %d",
                                    key,
                                    consumerRecord != null ? consumerRecord.partition() : -1,
                                    consumerRecord != null ? consumerRecord.offset() : -1),
                            exception
                    );
                },
                new FixedBackOff(1000L, 2)
        );

        errorHandler.addNotRetryableExceptions(
                org.apache.kafka.common.errors.SerializationException.class,
                org.springframework.kafka.support.serializer.DeserializationException.class
        );

        return errorHandler;
    }
}
