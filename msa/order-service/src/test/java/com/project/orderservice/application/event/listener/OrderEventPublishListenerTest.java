package com.project.orderservice.application.event.listener;

import com.project.orderservice.application.event.OrderCreatedInternalEvent;
import com.project.orderservice.application.event.service.OrderEventSendService;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("OrderEventPublishListener")
class OrderEventPublishListenerTest {

    @Test
    @DisplayName("AFTER_COMMIT 이벤트를 명명된 async executor로 받아 SendService에 위임한다")
    void publishOrderCreated_DelegatesToSendService() throws Exception {
        OrderEventSendService sendService = mock(OrderEventSendService.class);
        OrderEventPublishListener listener = new OrderEventPublishListener(sendService);
        OrderCreatedInternalEvent event = OrderCreatedInternalEvent.of(
                "order-1",
                10L,
                List.of(),
                1000
        );

        listener.publishOrderCreated(event);

        verify(sendService).sendOrderCreated(event);

        Method method = OrderEventPublishListener.class.getDeclaredMethod(
                "publishOrderCreated",
                OrderCreatedInternalEvent.class
        );
        Async async = method.getAnnotation(Async.class);
        TransactionalEventListener eventListener = method.getAnnotation(TransactionalEventListener.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertThat(async.value()).isEqualTo(OrderEventAsyncConfig.ORDER_EVENT_ASYNC_TASK_EXECUTOR);
        assertThat(eventListener.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }
}
