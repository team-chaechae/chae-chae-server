package com.project.paymentservice.application.service;

import com.project.paymentservice.application.event.PaymentCompletedInternalEvent;
import com.project.paymentservice.application.event.PaymentRefundedInternalEvent;
import com.project.paymentservice.application.response.ResPaymentDTO;
import com.project.paymentservice.domain.model.PaymentEntity;
import com.project.paymentservice.domain.model.PaymentStatus;
import com.project.paymentservice.domain.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentServiceImpl 단위 테스트")
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private String orderId;
    private Long salesId;
    private Integer amount;

    @BeforeEach
    void setUp() {
        orderId = "order-123";
        salesId = 1L;
        amount = 50000;
    }

    @Test
    @DisplayName("신규 결제 요청 시 결제가 정상적으로 생성된다")
    void processPayment_NewPayment_Success() {
        // given
        given(paymentRepository.findBySalesId(salesId)).willReturn(Optional.empty());
        given(paymentRepository.save(any(PaymentEntity.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        ResPaymentDTO result = paymentService.processPayment(orderId, salesId, amount);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPayment().getSalesId()).isEqualTo(salesId);
        assertThat(result.getPayment().getAmount()).isEqualTo(amount);
        assertThat(result.getPayment().getStatus()).isEqualTo(PaymentStatus.COMPLETED.name());

        verify(paymentRepository).findBySalesId(salesId);
        verify(paymentRepository).save(any(PaymentEntity.class));
        verify(eventPublisher).publishEvent(any(PaymentCompletedInternalEvent.class));
    }

    @Test
    @DisplayName("중복 결제 요청 시 기존 결제 정보를 반환한다")
    void processPayment_DuplicatePayment_ReturnsExisting() {
        // given
        PaymentEntity existingPayment = PaymentEntity.create(orderId, salesId, amount);
        existingPayment.process();
        existingPayment.complete();

        given(paymentRepository.findBySalesId(salesId)).willReturn(Optional.of(existingPayment));

        // when
        ResPaymentDTO result = paymentService.processPayment(orderId, salesId, amount);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPayment().getSalesId()).isEqualTo(salesId);
        assertThat(result.getPayment().getStatus()).isEqualTo(PaymentStatus.COMPLETED.name());

        verify(paymentRepository).findBySalesId(salesId);
        verify(paymentRepository, never()).save(any(PaymentEntity.class));
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("중복 결제 요청 시 다른 금액이어도 기존 결제 정보를 반환한다")
    void processPayment_DuplicateWithDifferentAmount_ReturnsExisting() {
        // given
        PaymentEntity existingPayment = PaymentEntity.create(orderId, salesId, 30000);
        existingPayment.process();
        existingPayment.complete();

        given(paymentRepository.findBySalesId(salesId)).willReturn(Optional.of(existingPayment));

        // when
        ResPaymentDTO result = paymentService.processPayment(orderId, salesId, 50000);

        // then
        assertThat(result.getPayment().getAmount()).isEqualTo(30000);
        verify(paymentRepository, never()).save(any(PaymentEntity.class));
    }

    @Test
    @DisplayName("실패한 결제가 있어도 기존 결제 정보를 반환한다")
    void processPayment_ExistingFailedPayment_ReturnsExisting() {
        // given
        PaymentEntity failedPayment = PaymentEntity.create(orderId, salesId, amount);
        failedPayment.process();
        failedPayment.fail("카드 한도 초과");

        given(paymentRepository.findBySalesId(salesId)).willReturn(Optional.of(failedPayment));

        // when
        ResPaymentDTO result = paymentService.processPayment(orderId, salesId, amount);

        // then
        assertThat(result.getPayment().getStatus()).isEqualTo(PaymentStatus.FAILED.name());
        verify(paymentRepository, never()).save(any(PaymentEntity.class));
    }

    @Test
    @DisplayName("환불 처리 시 이벤트가 발행된다")
    void refundPayment_PublishesEvent() {
        // given
        PaymentEntity payment = PaymentEntity.create(orderId, salesId, amount);
        payment.process();
        payment.complete();

        given(paymentRepository.findBySalesId(salesId)).willReturn(Optional.of(payment));

        // when
        paymentService.refundPayment(salesId, "재고 차감 실패");

        // then
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);

        ArgumentCaptor<PaymentRefundedInternalEvent> eventCaptor =
            ArgumentCaptor.forClass(PaymentRefundedInternalEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        PaymentRefundedInternalEvent event = eventCaptor.getValue();
        assertThat(event.getSalesId()).isEqualTo(salesId);
        assertThat(event.getOrderId()).isEqualTo(orderId);
    }
}
