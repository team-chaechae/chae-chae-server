package com.project.orderservice.infrastructure.sse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SseEmitterRegistry 단위 테스트
 *
 * SSE 연결 등록, 이벤트 전송, 연결 해제 기능 검증
 */
@DisplayName("SseEmitterRegistry 테스트")
class SseEmitterRegistryTest {

    private SseEmitterRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new SseEmitterRegistry();
    }

    @Test
    @DisplayName("SSE 연결 등록 시 Emitter 반환")
    void register_ShouldReturnEmitter() {
        // given
        String orderId = "order-123";

        // when
        SseEmitter emitter = registry.register(orderId);

        // then
        assertThat(emitter).isNotNull();
        assertThat(registry.getConnectionCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("동일 orderId로 재등록 시 기존 연결 교체")
    void register_SameOrderId_ShouldReplace() {
        // given
        String orderId = "order-123";

        // when
        SseEmitter emitter1 = registry.register(orderId);
        SseEmitter emitter2 = registry.register(orderId);

        // then
        assertThat(emitter1).isNotSameAs(emitter2);
        assertThat(registry.getConnectionCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("여러 orderId 등록 시 각각 관리")
    void register_MultipleOrderIds_ShouldManageSeparately() {
        // given & when
        registry.register("order-1");
        registry.register("order-2");
        registry.register("order-3");

        // then
        assertThat(registry.getConnectionCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("연결 해제 시 카운트 감소")
    void remove_ShouldDecreaseCount() {
        // given
        String orderId = "order-123";
        registry.register(orderId);
        assertThat(registry.getConnectionCount()).isEqualTo(1);

        // when
        registry.remove(orderId);

        // then
        assertThat(registry.getConnectionCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("존재하지 않는 orderId 해제 시 에러 없음")
    void remove_NonExistent_ShouldNotThrow() {
        // given
        String orderId = "non-existent";

        // when & then - 예외 발생하지 않아야 함
        registry.remove(orderId);
        assertThat(registry.getConnectionCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("존재하지 않는 orderId로 이벤트 전송 시 에러 없음")
    void sendEvent_NonExistent_ShouldNotThrow() {
        // given
        String orderId = "non-existent";
        NotificationEvent event = NotificationEvent.paymentConfirmed(orderId, 1L);

        // when & then - 예외 발생하지 않아야 함
        registry.sendEvent(orderId, event);
    }

    @Test
    @DisplayName("동시성 테스트 - 100개 동시 등록/해제")
    void concurrency_RegisterAndRemove_ShouldBeThreadSafe() throws InterruptedException {
        // given
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    String orderId = "order-" + index;
                    registry.register(orderId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 실패
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = endLatch.await(5, TimeUnit.SECONDS);

        // then
        assertThat(completed).isTrue();
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(registry.getConnectionCount()).isEqualTo(threadCount);

        executor.shutdown();
    }

    @Test
    @DisplayName("NotificationEvent - inventoryFailed 팩토리 메서드")
    void notificationEvent_InventoryFailed_ShouldCreateCorrectly() {
        // given
        String orderId = "order-123";
        Long salesId = 1L;
        String reason = "재고 부족";

        // when
        NotificationEvent event = NotificationEvent.inventoryFailed(orderId, salesId, reason);

        // then
        assertThat(event.getEventType()).isEqualTo("INVENTORY_FAILED");
        assertThat(event.getOrderId()).isEqualTo(orderId);
        assertThat(event.getSalesId()).isEqualTo(salesId);
        assertThat(event.getMessage()).isEqualTo(reason);
        assertThat(event.getOccurredAt()).isNotNull();
    }

    @Test
    @DisplayName("NotificationEvent - paymentConfirmed 팩토리 메서드")
    void notificationEvent_PaymentConfirmed_ShouldCreateCorrectly() {
        // given
        String orderId = "order-456";
        Long salesId = 2L;

        // when
        NotificationEvent event = NotificationEvent.paymentConfirmed(orderId, salesId);

        // then
        assertThat(event.getEventType()).isEqualTo("PAYMENT_CONFIRMED");
        assertThat(event.getOrderId()).isEqualTo(orderId);
        assertThat(event.getSalesId()).isEqualTo(salesId);
        assertThat(event.getMessage()).isEqualTo("결제가 완료되었습니다.");
        assertThat(event.getOccurredAt()).isNotNull();
    }
}
