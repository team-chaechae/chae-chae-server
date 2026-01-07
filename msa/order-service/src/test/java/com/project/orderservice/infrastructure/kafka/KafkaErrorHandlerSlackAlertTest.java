package com.project.orderservice.infrastructure.kafka;

import com.project.common.dlq.handler.DlqErrorHandler;
import com.project.orderservice.infrastructure.config.kafka.KafkaErrorHandlerConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.listener.CommonErrorHandler;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("KafkaErrorHandlerConfig - DLQ 핸들러 테스트")
class KafkaErrorHandlerSlackAlertTest {

    @Mock
    private DlqErrorHandler dlqErrorHandler;

    @Test
    @DisplayName("DlqErrorHandler가 CommonErrorHandler로 주입된다")
    void kafkaErrorHandlerConfig_ShouldReturnDlqErrorHandler() {
        KafkaErrorHandlerConfig config = new KafkaErrorHandlerConfig();
        CommonErrorHandler handler = config.kafkaErrorHandler(dlqErrorHandler);

        assertThat(handler).isSameAs(dlqErrorHandler);
    }
}
