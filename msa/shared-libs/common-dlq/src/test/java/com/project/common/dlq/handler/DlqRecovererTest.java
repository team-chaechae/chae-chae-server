package com.project.common.dlq.handler;

import com.project.common.dlq.alert.DlqAlertService;
import com.project.common.dlq.config.DlqProperties;
import com.project.common.dlq.domain.DlqMessage;
import com.project.common.dlq.domain.DlqRecord;
import com.project.common.dlq.domain.DlqRecordRepository;
import com.project.common.dlq.exception.DlqExceptionClassifier;
import com.project.common.dlq.producer.DlqMessageProducer;
import com.project.common.dlq.exception.BaseErrorCode;
import com.project.common.dlq.exception.BusinessException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("DlqRecoverer")
class DlqRecovererTest {

    @Test
    @DisplayName("비즈니스 예외면 DLQ 발행 + DB 저장 + 알림")
    void accept_businessException_sendsToDlqAndSavesDb() {
        DlqMessageProducer producer = mock(DlqMessageProducer.class);
        DlqAlertService alertService = mock(DlqAlertService.class);
        DlqRecordRepository repository = mock(DlqRecordRepository.class);
        DlqExceptionClassifier classifier = new DlqExceptionClassifier();

        DlqProperties properties = new DlqProperties();
        properties.setDbEnabled(true);

        DlqRecoverer recoverer = new DlqRecoverer(
                producer,
                alertService,
                classifier,
                repository,
                properties,
                "payment-service"
        );

        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("order-created", 0, 123L, "key-1", "{\"foo\":\"bar\"}");

        recoverer.accept(record, new BusinessException(TestErrorCode.PAYMENT_FAILED, "결제 실패"));

        verify(producer, times(1)).sendToDlq(any(DlqMessage.class));
        verify(repository, times(1)).save(any(DlqRecord.class));
        verify(alertService, times(1)).sendDlqAlert(any(DlqMessage.class));
    }

    @Test
    @DisplayName("기술적 예외면 DLQ 발행 + 알림 (DB 저장 없음)")
    void accept_technicalException_sendsToDlqWithoutDbSave() {
        DlqMessageProducer producer = mock(DlqMessageProducer.class);
        DlqAlertService alertService = mock(DlqAlertService.class);
        DlqRecordRepository repository = mock(DlqRecordRepository.class);
        DlqExceptionClassifier classifier = new DlqExceptionClassifier();

        DlqProperties properties = new DlqProperties();
        properties.setDbEnabled(true);

        DlqRecoverer recoverer = new DlqRecoverer(
                producer,
                alertService,
                classifier,
                repository,
                properties,
                "inventory-service"
        );

        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("payment-completed", 1, 456L, "key-2", "{\"bar\":\"baz\"}");

        recoverer.accept(record, new java.net.SocketTimeoutException("timeout"));

        verify(producer, times(1)).sendToDlq(any(DlqMessage.class));
        verify(repository, never()).save(any(DlqRecord.class));
        verify(alertService, times(1)).sendDlqAlert(any(DlqMessage.class));
    }

    @Test
    @DisplayName("DLQ 발행 실패 시 DB 저장과 알림을 진행하지 않고 예외를 전파한다")
    void accept_dlqPublishFails_propagatesException() {
        DlqMessageProducer producer = mock(DlqMessageProducer.class);
        DlqAlertService alertService = mock(DlqAlertService.class);
        DlqRecordRepository repository = mock(DlqRecordRepository.class);
        DlqExceptionClassifier classifier = new DlqExceptionClassifier();

        DlqProperties properties = new DlqProperties();
        properties.setDbEnabled(true);

        DlqRecoverer recoverer = new DlqRecoverer(
                producer,
                alertService,
                classifier,
                repository,
                properties,
                "payment-service"
        );

        ConsumerRecord<String, String> record =
                new ConsumerRecord<>("order-created", 0, 123L, "key-1", "{\"foo\":\"bar\"}");

        doThrow(new IllegalStateException("DLQ publish failed"))
                .when(producer).sendToDlq(any(DlqMessage.class));

        assertThatThrownBy(() -> recoverer.accept(record, new RuntimeException("processing failed")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DLQ publish failed");

        verify(repository, never()).save(any(DlqRecord.class));
        verify(alertService, never()).sendDlqAlert(any(DlqMessage.class));
    }

    private enum TestErrorCode implements BaseErrorCode {
        PAYMENT_FAILED;

        @Override
        public HttpStatus getHttpStatus() {
            return HttpStatus.BAD_REQUEST;
        }

        @Override
        public String getCode() {
            return "PAYMENT_FAILED";
        }

        @Override
        public String getMessage() {
            return "결제 실패";
        }
    }
}
