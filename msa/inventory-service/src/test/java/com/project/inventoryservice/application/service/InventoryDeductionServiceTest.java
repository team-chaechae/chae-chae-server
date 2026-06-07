package com.project.inventoryservice.application.service;

import com.project.inventoryservice.infrastructure.kafka.InventoryEvent;
import com.project.inventoryservice.infrastructure.kafka.InventoryConfirmedEventProducer;
import com.project.inventoryservice.infrastructure.kafka.InventoryEventProducer;
import com.project.inventoryservice.infrastructure.kafka.InventoryFailedEventProducer;
import com.project.inventoryservice.infrastructure.kafka.dto.InventoryFailedEvent;
import com.project.inventoryservice.infrastructure.kafka.dto.PaymentCompletedEvent;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryDeductionService 테스트")
class InventoryDeductionServiceTest {

    @Mock
    private StockCacheService stockCacheService;

    @Mock
    private InventoryEventProducer inventoryEventProducer;

    @Mock
    private InventoryConfirmedEventProducer inventoryConfirmedEventProducer;

    @Mock
    private InventoryFailedEventProducer inventoryFailedEventProducer;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RBucket<String> rBucket;

    @InjectMocks
    private InventoryDeductionService inventoryDeductionService;

    @Captor
    private ArgumentCaptor<InventoryEvent> inventoryEventCaptor;

    @Captor
    private ArgumentCaptor<InventoryFailedEvent> failedEventCaptor;

    private String orderId;
    private Long salesId;

    @BeforeEach
    void setUp() {
        orderId = "order-test-123";
        salesId = 1L;
        lenient().when(redissonClient.<String>getBucket(anyString())).thenReturn(rBucket);
        lenient().when(rBucket.setIfAbsent(anyString(), any())).thenReturn(true);
    }

    @Nested
    @DisplayName("재고 차감 성공 테스트")
    class DeductionSuccessTest {

        @Test
        @DisplayName("단일 상품 재고 차감이 성공한다")
        void processPaymentCompleted_SingleItem_Success() {
            // given
            PaymentCompletedEvent event = createPaymentCompletedEvent(
                    List.of(createOrderItem(1L, "상품A", 2, 10000))
            );
            given(stockCacheService.decreaseStock(1L, 2)).willReturn(98);

            // when
            inventoryDeductionService.processPaymentCompleted(event);

            // then
            verify(stockCacheService).decreaseStock(1L, 2);
            verify(inventoryEventProducer).publish(inventoryEventCaptor.capture());

            InventoryEvent publishedEvent = inventoryEventCaptor.getValue();
            assertThat(publishedEvent.getProductId()).isEqualTo(1L);
            assertThat(publishedEvent.getQuantity()).isEqualTo(-2);
            assertThat(publishedEvent.getChangeType()).isEqualTo("ORDER_DECREASE");
            assertThat(publishedEvent.getStatus()).isEqualTo("CONFIRMED");
            assertThat(publishedEvent.getCurrentStock()).isEqualTo(98);
        }

        @Test
        @DisplayName("여러 상품 재고 차감이 모두 성공한다")
        void processPaymentCompleted_MultipleItems_AllSuccess() {
            // given
            PaymentCompletedEvent event = createPaymentCompletedEvent(Arrays.asList(
                    createOrderItem(1L, "상품A", 2, 10000),
                    createOrderItem(2L, "상품B", 3, 20000),
                    createOrderItem(3L, "상품C", 1, 15000)
            ));

            given(stockCacheService.decreaseStock(1L, 2)).willReturn(98);
            given(stockCacheService.decreaseStock(2L, 3)).willReturn(47);
            given(stockCacheService.decreaseStock(3L, 1)).willReturn(199);

            // when
            inventoryDeductionService.processPaymentCompleted(event);

            // then
            verify(stockCacheService, times(3)).decreaseStock(anyLong(), anyInt());
            verify(inventoryEventProducer, times(3)).publish(any(InventoryEvent.class));
            verify(inventoryFailedEventProducer, never()).publish(any());
        }
    }

    @Nested
    @DisplayName("재고 차감 실패 및 롤백 테스트")
    class DeductionFailureAndRollbackTest {

        @Test
        @DisplayName("재고 부족 시 이미 차감된 재고가 롤백된다")
        void processPaymentCompleted_InsufficientStock_RollsBack() {
            // given
            PaymentCompletedEvent event = createPaymentCompletedEvent(Arrays.asList(
                    createOrderItem(1L, "상품A", 2, 10000),
                    createOrderItem(2L, "상품B", 100, 20000),  // 재고 부족
                    createOrderItem(3L, "상품C", 1, 15000)
            ));

            given(stockCacheService.decreaseStock(1L, 2)).willReturn(98);
            given(stockCacheService.decreaseStock(2L, 100))
                    .willThrow(new RuntimeException("재고 부족"));

            // when
            inventoryDeductionService.processPaymentCompleted(event);

            // then
            // 첫 번째 상품만 차감 시도
            verify(stockCacheService).decreaseStock(1L, 2);
            verify(stockCacheService).decreaseStock(2L, 100);
            // 세 번째 상품은 시도하지 않음
            verify(stockCacheService, never()).decreaseStock(eq(3L), anyInt());

            // 첫 번째 상품 롤백
            verify(stockCacheService).increaseStock(1L, 2);

            // 실패 이벤트 발행
            verify(inventoryFailedEventProducer).publish(failedEventCaptor.capture());
            InventoryFailedEvent failedEvent = failedEventCaptor.getValue();
            assertThat(failedEvent.getOrderId()).isEqualTo(orderId);
            assertThat(failedEvent.getSalesId()).isEqualTo(salesId);
            assertThat(failedEvent.getReason()).contains("상품 2");
        }

        @Test
        @DisplayName("첫 번째 상품 차감 실패 시 롤백 없이 실패 이벤트만 발행")
        void processPaymentCompleted_FirstItemFails_NoRollbackNeeded() {
            // given
            PaymentCompletedEvent event = createPaymentCompletedEvent(Arrays.asList(
                    createOrderItem(1L, "상품A", 100, 10000),  // 첫 번째부터 실패
                    createOrderItem(2L, "상품B", 3, 20000)
            ));

            given(stockCacheService.decreaseStock(1L, 100))
                    .willThrow(new RuntimeException("재고 부족"));

            // when
            inventoryDeductionService.processPaymentCompleted(event);

            // then
            // 롤백 호출 없음 (차감된 것이 없으므로)
            verify(stockCacheService, never()).increaseStock(anyLong(), anyInt());

            // 실패 이벤트 발행
            verify(inventoryFailedEventProducer).publish(any(InventoryFailedEvent.class));
        }

        @Test
        @DisplayName("Redis timeout 같은 기술 장애도 현재는 재고 실패 이벤트로 발행되어 환불 플로우로 이어진다")
        void processPaymentCompleted_TechnicalFailure_CurrentlyPublishesInventoryFailed() {
            // given
            PaymentCompletedEvent event = createPaymentCompletedEvent(List.of(
                    createOrderItem(1L, "상품A", 2, 10000)
            ));

            given(stockCacheService.decreaseStock(1L, 2))
                    .willThrow(new RuntimeException("Redis timeout"));

            // when
            inventoryDeductionService.processPaymentCompleted(event);

            // then
            verify(inventoryFailedEventProducer).publish(failedEventCaptor.capture());
            InventoryFailedEvent failedEvent = failedEventCaptor.getValue();
            assertThat(failedEvent.getOrderId()).isEqualTo(orderId);
            assertThat(failedEvent.getSalesId()).isEqualTo(salesId);
            assertThat(failedEvent.getReason()).contains("Redis timeout");
            verify(rBucket).delete();
        }

        @Test
        @DisplayName("롤백 중 예외 발생해도 계속 처리된다")
        void processPaymentCompleted_RollbackFailure_ContinuesProcessing() {
            // given
            PaymentCompletedEvent event = createPaymentCompletedEvent(Arrays.asList(
                    createOrderItem(1L, "상품A", 2, 10000),
                    createOrderItem(2L, "상품B", 3, 20000),
                    createOrderItem(3L, "상품C", 100, 15000)  // 재고 부족
            ));

            given(stockCacheService.decreaseStock(1L, 2)).willReturn(98);
            given(stockCacheService.decreaseStock(2L, 3)).willReturn(47);
            given(stockCacheService.decreaseStock(3L, 100))
                    .willThrow(new RuntimeException("재고 부족"));

            // 첫 번째 롤백 실패
            doThrow(new RuntimeException("Redis connection failed"))
                    .when(stockCacheService).increaseStock(1L, 2);

            // when
            inventoryDeductionService.processPaymentCompleted(event);

            // then
            // 첫 번째 롤백 실패해도 두 번째 롤백 시도
            verify(stockCacheService).increaseStock(1L, 2);
            verify(stockCacheService).increaseStock(2L, 3);

            // 실패 이벤트는 발행됨
            verify(inventoryFailedEventProducer).publish(any(InventoryFailedEvent.class));
        }
    }

    @Nested
    @DisplayName("엣지 케이스 테스트")
    class EdgeCaseTest {

        @Test
        @DisplayName("상품 목록이 null이면 스킵된다")
        void processPaymentCompleted_NullItems_Skipped() {
            // given
            PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                    .eventId("event-null-test")
                    .orderId(orderId)
                    .salesId(salesId)
                    .items(null)
                    .totalAmount(0)
                    .completedAt(LocalDateTime.now())
                    .build();

            // when
            inventoryDeductionService.processPaymentCompleted(event);

            // then
            verify(stockCacheService, never()).decreaseStock(anyLong(), anyInt());
            verify(inventoryEventProducer, never()).publish(any());
            verify(inventoryFailedEventProducer, never()).publish(any());
        }

        @Test
        @DisplayName("상품 목록이 비어있으면 스킵된다")
        void processPaymentCompleted_EmptyItems_Skipped() {
            // given
            PaymentCompletedEvent event = createPaymentCompletedEvent(Collections.emptyList());

            // when
            inventoryDeductionService.processPaymentCompleted(event);

            // then
            verify(stockCacheService, never()).decreaseStock(anyLong(), anyInt());
            verify(inventoryEventProducer, never()).publish(any());
            verify(inventoryFailedEventProducer, never()).publish(any());
        }
    }

    @Nested
    @DisplayName("멱등성 테스트 시나리오")
    class IdempotencyScenarioTest {

        @Test
        @DisplayName("동일한 이벤트가 여러 번 와도 재고는 한 번만 차감되어야 한다 (멱등성 키 필요)")
        void processPaymentCompleted_Idempotency_Consideration() {
            // given - 같은 orderId로 두 번 호출
            PaymentCompletedEvent event = createPaymentCompletedEvent(
                    List.of(createOrderItem(1L, "상품A", 2, 10000))
            );
            given(rBucket.setIfAbsent(anyString(), any())).willReturn(true, false);
            given(stockCacheService.decreaseStock(1L, 2)).willReturn(98);

            // when - 첫 번째 호출
            inventoryDeductionService.processPaymentCompleted(event);

            // when - 두 번째 호출 (중복)
            inventoryDeductionService.processPaymentCompleted(event);

            // then - 두 번째 이벤트는 멱등성 키로 스킵된다.
            verify(stockCacheService, times(1)).decreaseStock(1L, 2);
        }
    }

    // 헬퍼 메서드
    private PaymentCompletedEvent createPaymentCompletedEvent(List<PaymentCompletedEvent.OrderItem> items) {
        return PaymentCompletedEvent.builder()
                .eventId("event-" + orderId)
                .orderId(orderId)
                .salesId(salesId)
                .items(items)
                .totalAmount(items.stream()
                        .mapToInt(item -> item.getPrice() * item.getQuantity())
                        .sum())
                .completedAt(LocalDateTime.now())
                .build();
    }

    private PaymentCompletedEvent.OrderItem createOrderItem(Long productId, String name, int quantity, int price) {
        return PaymentCompletedEvent.OrderItem.builder()
                .productId(productId)
                .productName(name)
                .quantity(quantity)
                .price(price)
                .build();
    }
}
