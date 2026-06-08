package com.project.paymentservice.application.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Payment 이벤트 Kafka key")
class PaymentEventKeyTest {

    @Test
    @DisplayName("결제 완료 이벤트는 salesId를 message key로 사용한다")
    void paymentCompletedEvent_UsesSalesIdAsMessageKey() {
        PaymentCompletedInternalEvent event = PaymentCompletedInternalEvent.of(
                "order-7",
                7L,
                50000,
                List.of()
        );

        assertThat(event.getMessageKey()).isEqualTo("7");
    }

    @Test
    @DisplayName("결제 환불 이벤트는 salesId를 message key로 사용한다")
    void paymentRefundedEvent_UsesSalesIdAsMessageKey() {
        PaymentRefundedInternalEvent event = PaymentRefundedInternalEvent.of(
                "order-7",
                7L,
                "재고 차감 실패"
        );

        assertThat(event.getMessageKey()).isEqualTo("7");
    }
}
