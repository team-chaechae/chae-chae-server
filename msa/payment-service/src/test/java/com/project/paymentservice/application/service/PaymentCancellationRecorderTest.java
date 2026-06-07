package com.project.paymentservice.application.service;

import com.project.paymentservice.application.event.PaymentRefundedInternalEvent;
import com.project.paymentservice.application.response.ResPaymentDTO;
import com.project.paymentservice.domain.model.PaymentEntity;
import com.project.paymentservice.domain.model.PaymentStatus;
import com.project.paymentservice.domain.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentCancellationRecorder 단위 테스트")
class PaymentCancellationRecorderTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PaymentCancellationRecorder paymentCancellationRecorder;

    @Test
    @DisplayName("결제 환불 기록 시 상태를 REFUNDED로 변경하고 환불 이벤트를 발행한다")
    void recordPaymentRefunded_UpdatesStatusAndPublishesEvent() {
        // given
        Long salesId = 1L;
        String orderId = "order-123";
        PaymentEntity payment = PaymentEntity.create(orderId, salesId, 50000);
        payment.process();
        payment.complete();

        given(paymentRepository.findBySalesId(salesId)).willReturn(Optional.of(payment));

        // when
        ResPaymentDTO result = paymentCancellationRecorder.recordPaymentRefunded(salesId, "고객 요청");

        // then
        assertThat(result.getPayment().getStatus()).isEqualTo(PaymentStatus.REFUNDED.name());
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);

        ArgumentCaptor<PaymentRefundedInternalEvent> eventCaptor =
                ArgumentCaptor.forClass(PaymentRefundedInternalEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getSalesId()).isEqualTo(salesId);
        assertThat(eventCaptor.getValue().getOrderId()).isEqualTo(orderId);
    }
}
