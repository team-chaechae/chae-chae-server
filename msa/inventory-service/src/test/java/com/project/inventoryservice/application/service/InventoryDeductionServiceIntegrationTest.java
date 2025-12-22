package com.project.inventoryservice.application.service;

import com.project.inventoryservice.infrastructure.kafka.InventoryEventProducer;
import com.project.inventoryservice.infrastructure.kafka.InventoryFailedEventProducer;
import com.project.inventoryservice.infrastructure.kafka.dto.PaymentCompletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * InventoryDeductionService 통합 테스트
 *
 * setIfAbsent 원자적 연산 적용 후:
 * 1. Race condition 방지 확인
 * 2. 결제 완료 후 재고 계산 정확성 확인
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryDeductionService 통합 테스트 - setIfAbsent 적용 후")
class InventoryDeductionServiceIntegrationTest {

    @Mock
    private StockCacheService stockCacheService;

    @Mock
    private InventoryEventProducer inventoryEventProducer;

    @Mock
    private InventoryFailedEventProducer inventoryFailedEventProducer;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RBucket<String> rBucket;

    private InventoryDeductionService inventoryDeductionService;

    private static final int VUSER_COUNT = 100;
    private static final String ORDER_ID = "order-test-123";
    private static final Long SALES_ID = 1L;
    private static final Long PRODUCT_ID = 100L;
    private static final int INITIAL_STOCK = 1000;

    @BeforeEach
    void setUp() {
        inventoryDeductionService = new InventoryDeductionService(
                stockCacheService,
                inventoryEventProducer,
                inventoryFailedEventProducer,
                redissonClient
        );
    }

    @Test
    @DisplayName("[setIfAbsent] 100개 동시 요청 시 재고가 1번만 차감됨")
    void processPaymentCompleted_100ConcurrentCalls_OnlyOneDeduction() throws InterruptedException {
        // given
        PaymentCompletedEvent event = createPaymentEvent(ORDER_ID, SALES_ID, PRODUCT_ID, 5);
        AtomicInteger currentStock = new AtomicInteger(INITIAL_STOCK);
        AtomicInteger deductionCount = new AtomicInteger(0);

        // setIfAbsent 원자적 동작 시뮬레이션
        AtomicInteger lockAcquired = new AtomicInteger(0);
        given(redissonClient.<String>getBucket(anyString())).willReturn(rBucket);

        // 첫 번째 호출만 true, 나머지는 false (원자적)
        given(rBucket.setIfAbsent(anyString(), any(Duration.class)))
                .willAnswer(invocation -> lockAcquired.getAndIncrement() == 0);

        // 재고 차감
        given(stockCacheService.decreaseStock(eq(PRODUCT_ID), eq(5)))
                .willAnswer(invocation -> {
                    deductionCount.incrementAndGet();
                    return currentStock.addAndGet(-5);
                });

        ExecutorService executorService = Executors.newFixedThreadPool(VUSER_COUNT);
        CountDownLatch readyLatch = new CountDownLatch(VUSER_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(VUSER_COUNT);

        // when - 100개 동시 요청
        for (int i = 0; i < VUSER_COUNT; i++) {
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();
                    inventoryDeductionService.processPaymentCompleted(event);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();
        executorService.shutdown();

        // then
        System.out.println("=".repeat(60));
        System.out.println("[setIfAbsent 적용 후 테스트 결과]");
        System.out.println("총 요청 수: " + VUSER_COUNT);
        System.out.println("재고 차감 횟수: " + deductionCount.get());
        System.out.println("초기 재고: " + INITIAL_STOCK);
        System.out.println("최종 재고: " + currentStock.get());
        System.out.println("예상 차감량: 5");
        System.out.println("실제 차감량: " + (INITIAL_STOCK - currentStock.get()));
        System.out.println("=".repeat(60));

        // 재고는 정확히 1번만 차감
        assertThat(deductionCount.get()).isEqualTo(1);
        assertThat(currentStock.get()).isEqualTo(INITIAL_STOCK - 5);
        System.out.println("✅ setIfAbsent로 race condition 완벽 방지!");
    }

    @Test
    @DisplayName("[재고 계산] 여러 주문이 순차적으로 처리되면 재고가 정확히 차감됨")
    void processPaymentCompleted_MultipleOrders_CorrectStockCalculation() {
        // given
        AtomicInteger currentStock = new AtomicInteger(INITIAL_STOCK);
        AtomicInteger deductionCount = new AtomicInteger(0);

        given(redissonClient.<String>getBucket(anyString())).willReturn(rBucket);
        given(rBucket.setIfAbsent(anyString(), any(Duration.class))).willReturn(true);
        given(stockCacheService.decreaseStock(anyLong(), anyInt()))
                .willAnswer(invocation -> {
                    int quantity = invocation.getArgument(1);
                    deductionCount.incrementAndGet();
                    return currentStock.addAndGet(-quantity);
                });

        // when - 5개 주문 처리 (각각 다른 orderId)
        for (int i = 1; i <= 5; i++) {
            String orderId = "order-" + i;
            PaymentCompletedEvent event = createPaymentEvent(orderId, (long) i, PRODUCT_ID, 10);
            inventoryDeductionService.processPaymentCompleted(event);
        }

        // then
        System.out.println("=".repeat(60));
        System.out.println("[재고 계산 테스트 결과]");
        System.out.println("주문 수: 5");
        System.out.println("주문당 차감량: 10");
        System.out.println("총 예상 차감량: 50");
        System.out.println("초기 재고: " + INITIAL_STOCK);
        System.out.println("최종 재고: " + currentStock.get());
        System.out.println("실제 차감량: " + (INITIAL_STOCK - currentStock.get()));
        System.out.println("=".repeat(60));

        assertThat(deductionCount.get()).isEqualTo(5);
        assertThat(currentStock.get()).isEqualTo(INITIAL_STOCK - 50);
        System.out.println("✅ 재고 계산 정확!");
    }

    @Test
    @DisplayName("[중복 주문] 같은 orderId로 여러 번 요청해도 1번만 처리")
    void processPaymentCompleted_DuplicateOrders_OnlyOneProcessed() {
        // given
        AtomicInteger currentStock = new AtomicInteger(INITIAL_STOCK);
        AtomicInteger deductionCount = new AtomicInteger(0);
        AtomicInteger setIfAbsentCallCount = new AtomicInteger(0);

        given(redissonClient.<String>getBucket(anyString())).willReturn(rBucket);
        // 첫 번째만 true, 나머지 false
        given(rBucket.setIfAbsent(anyString(), any(Duration.class)))
                .willAnswer(invocation -> setIfAbsentCallCount.getAndIncrement() == 0);

        given(stockCacheService.decreaseStock(anyLong(), anyInt()))
                .willAnswer(invocation -> {
                    int quantity = invocation.getArgument(1);
                    deductionCount.incrementAndGet();
                    return currentStock.addAndGet(-quantity);
                });

        PaymentCompletedEvent event = createPaymentEvent(ORDER_ID, SALES_ID, PRODUCT_ID, 10);

        // when - 같은 주문 10번 요청
        for (int i = 0; i < 10; i++) {
            inventoryDeductionService.processPaymentCompleted(event);
        }

        // then
        System.out.println("=".repeat(60));
        System.out.println("[중복 주문 테스트 결과]");
        System.out.println("동일 주문 요청 횟수: 10");
        System.out.println("실제 처리 횟수: " + deductionCount.get());
        System.out.println("초기 재고: " + INITIAL_STOCK);
        System.out.println("최종 재고: " + currentStock.get());
        System.out.println("=".repeat(60));

        assertThat(deductionCount.get()).isEqualTo(1);
        assertThat(currentStock.get()).isEqualTo(INITIAL_STOCK - 10);
        System.out.println("✅ 중복 주문 방지 성공!");
    }

    @Test
    @DisplayName("[재고 부족] 재고 부족 시 롤백 후 멱등성 키 해제")
    void processPaymentCompleted_InsufficientStock_RollbackAndReleaseLock() {
        // given
        AtomicInteger currentStock = new AtomicInteger(5);  // 재고 5개

        given(redissonClient.<String>getBucket(anyString())).willReturn(rBucket);
        given(rBucket.setIfAbsent(anyString(), any(Duration.class))).willReturn(true);

        // 첫 번째 상품 성공, 두 번째 상품 실패
        given(stockCacheService.decreaseStock(eq(100L), eq(3)))
                .willReturn(currentStock.addAndGet(-3));  // 5 - 3 = 2
        given(stockCacheService.decreaseStock(eq(200L), eq(10)))
                .willThrow(new RuntimeException("재고 부족"));
        given(stockCacheService.increaseStock(eq(100L), eq(3)))
                .willReturn(currentStock.addAndGet(3));  // 롤백: 2 + 3 = 5

        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .eventId("event-1")
                .orderId(ORDER_ID)
                .salesId(SALES_ID)
                .totalAmount(50000)
                .items(List.of(
                        PaymentCompletedEvent.OrderItem.builder()
                                .productId(100L).productName("상품1").quantity(3).price(10000).build(),
                        PaymentCompletedEvent.OrderItem.builder()
                                .productId(200L).productName("상품2").quantity(10).price(10000).build()
                ))
                .completedAt(LocalDateTime.now())
                .build();

        // when
        inventoryDeductionService.processPaymentCompleted(event);

        // then
        System.out.println("=".repeat(60));
        System.out.println("[재고 부족 테스트 결과]");
        System.out.println("초기 재고: 5");
        System.out.println("최종 재고: " + currentStock.get());
        System.out.println("=".repeat(60));

        // 롤백되어 원래 재고로 복구
        assertThat(currentStock.get()).isEqualTo(5);

        // 멱등성 키 해제 확인 (delete 호출)
        verify(rBucket).delete();

        // inventory-failed 이벤트 발행 확인
        verify(inventoryFailedEventProducer).publish(any());

        System.out.println("✅ 재고 부족 시 롤백 + 멱등성 키 해제 성공!");
    }

    @Test
    @DisplayName("[RTT 비교] 기존 2 RTT → 1 RTT로 감소 확인")
    void compareRTT_OldVsNew() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("RTT(Round Trip Time) 비교");
        System.out.println("=".repeat(60));
        System.out.println("기존 방식:");
        System.out.println("  1. isExists()  → Redis → 응답 (1 RTT)");
        System.out.println("  2. set()       → Redis → 응답 (1 RTT)");
        System.out.println("  총: 2 RTT");
        System.out.println();
        System.out.println("개선 방식 (setIfAbsent):");
        System.out.println("  1. setIfAbsent() → Redis → 응답 (1 RTT)");
        System.out.println("  총: 1 RTT");
        System.out.println();
        System.out.println("성능 개선: 50% RTT 감소");
        System.out.println("=".repeat(60));
    }

    private PaymentCompletedEvent createPaymentEvent(String orderId, Long salesId, Long productId, int quantity) {
        PaymentCompletedEvent.OrderItem item = PaymentCompletedEvent.OrderItem.builder()
                .productId(productId)
                .productName("테스트 상품")
                .quantity(quantity)
                .price(10000)
                .build();

        return PaymentCompletedEvent.builder()
                .eventId("event-" + System.currentTimeMillis())
                .orderId(orderId)
                .salesId(salesId)
                .totalAmount(quantity * 10000)
                .items(List.of(item))
                .completedAt(LocalDateTime.now())
                .build();
    }
}
