package com.project.orderservice.application.service;

import com.project.orderservice.domain.model.DeliveryAddressSnapshot;
import com.project.orderservice.domain.model.SalesEntity;
import com.project.orderservice.domain.model.SalesItemEntity;
import com.project.orderservice.domain.model.SalesStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SalesService Race Condition 테스트
 *
 * 백프레셔(Semaphore) 적용으로 순서 보장이 안 되는 상황에서
 * 멱등성이 제대로 동작하는지 검증
 *
 * 문제: isCompleted() 체크 후 complete() 호출 사이에 race condition 발생 가능
 */
@DisplayName("SalesService Race Condition 테스트 (100 vuser)")
class SalesServiceRaceConditionTest {

    private static final int VUSER_COUNT = 100;
    private static final String ORDER_ID = "order-race-test";
    private static final Long SALES_ID = 1L;

    private SalesEntity createPendingSales() {
        SalesItemEntity item = SalesItemEntity.create(1L, "테스트 상품", 1, 10000);
        SalesEntity sales = SalesEntity.createWithItems(ORDER_ID, 1L, deliveryAddress(), List.of(item));
        ReflectionTestUtils.setField(sales, "id", SALES_ID);
        return sales;
    }

    private DeliveryAddressSnapshot deliveryAddress() {
        return DeliveryAddressSnapshot.create(
                "테스트 수령인",
                "010-0000-0000",
                "00000",
                "테스트 주소",
                null,
                null
        );
    }

    @Test
    @DisplayName("[Race Condition 발생] 100개 동시 요청 시 complete()가 여러 번 호출됨")
    void completeSales_100ConcurrentCalls_RaceConditionOccurs() throws InterruptedException {
        // given
        SalesEntity sharedSales = createPendingSales();

        ExecutorService executorService = Executors.newFixedThreadPool(VUSER_COUNT);
        CountDownLatch readyLatch = new CountDownLatch(VUSER_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(VUSER_COUNT);

        AtomicInteger completeCallCount = new AtomicInteger(0);
        AtomicInteger skipCount = new AtomicInteger(0);

        // when - 100개 스레드가 동시에 completeSales 호출
        for (int i = 0; i < VUSER_COUNT; i++) {
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();  // 모든 스레드가 동시에 시작

                    // Race Condition 시뮬레이션:
                    // 실제 SalesServiceImpl.completeSales()의 로직을 재현
                    // 락이 없으면 여러 스레드가 isCompleted() false를 보고 complete() 호출
                    if (sharedSales.isCompleted()) {
                        skipCount.incrementAndGet();
                    } else {
                        // 여기서 race condition 발생!
                        // 여러 스레드가 동시에 이 블록에 진입 가능
                        Thread.sleep(1);  // 실제 DB I/O 시뮬레이션
                        sharedSales.complete();
                        completeCallCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();  // 동시 시작!
        doneLatch.await();
        executorService.shutdown();

        // then
        System.out.println("=".repeat(60));
        System.out.println("[Race Condition 테스트 결과]");
        System.out.println("총 요청 수: " + VUSER_COUNT);
        System.out.println("complete() 호출 횟수: " + completeCallCount.get());
        System.out.println("스킵 횟수: " + skipCount.get());
        System.out.println("=".repeat(60));

        // Race condition이 발생하면 completeCallCount > 1
        // 멱등성이 완벽하면 completeCallCount == 1 이어야 함
        if (completeCallCount.get() > 1) {
            System.out.println("⚠️  RACE CONDITION 발생! complete()가 " + completeCallCount.get() + "번 호출됨");
            System.out.println("    → 비관적 락 또는 낙관적 락 필요");
        } else {
            System.out.println("✅ 멱등성 정상 동작 (1번만 호출됨)");
        }

        // 현재 구현에서는 race condition이 발생할 수 있음을 검증
        // 이 테스트는 문제를 "발견"하기 위한 것
        assertThat(sharedSales.getStatus()).isEqualTo(SalesStatus.COMPLETED);
    }

    @Test
    @DisplayName("[동기화 적용 시] synchronized 블록으로 race condition 방지")
    void completeSales_100ConcurrentCalls_WithSynchronization_NoRaceCondition() throws InterruptedException {
        // given
        SalesEntity sharedSales = createPendingSales();
        Object lock = new Object();  // 락 객체

        ExecutorService executorService = Executors.newFixedThreadPool(VUSER_COUNT);
        CountDownLatch readyLatch = new CountDownLatch(VUSER_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(VUSER_COUNT);

        AtomicInteger completeCallCount = new AtomicInteger(0);
        AtomicInteger skipCount = new AtomicInteger(0);

        // when - synchronized로 동기화
        for (int i = 0; i < VUSER_COUNT; i++) {
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    synchronized (lock) {  // 비관적 락 시뮬레이션
                        if (sharedSales.isCompleted()) {
                            skipCount.incrementAndGet();
                        } else {
                            Thread.sleep(1);
                            sharedSales.complete();
                            completeCallCount.incrementAndGet();
                        }
                    }
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
        System.out.println("[동기화 적용 테스트 결과]");
        System.out.println("총 요청 수: " + VUSER_COUNT);
        System.out.println("complete() 호출 횟수: " + completeCallCount.get());
        System.out.println("스킵 횟수: " + skipCount.get());
        System.out.println("=".repeat(60));

        // 동기화 적용 시 정확히 1번만 호출
        assertThat(completeCallCount.get()).isEqualTo(1);
        assertThat(skipCount.get()).isEqualTo(VUSER_COUNT - 1);
        System.out.println("✅ 동기화로 race condition 방지 성공");
    }

    @Test
    @DisplayName("[cancelSales Race Condition] 100개 동시 취소 요청")
    void cancelSales_100ConcurrentCalls_RaceConditionOccurs() throws InterruptedException {
        // given
        SalesEntity sharedSales = createPendingSales();

        ExecutorService executorService = Executors.newFixedThreadPool(VUSER_COUNT);
        CountDownLatch readyLatch = new CountDownLatch(VUSER_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(VUSER_COUNT);

        AtomicInteger cancelCallCount = new AtomicInteger(0);
        AtomicInteger skipCount = new AtomicInteger(0);

        // when
        for (int i = 0; i < VUSER_COUNT; i++) {
            final int threadNum = i;
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    if (sharedSales.isCancelled()) {
                        skipCount.incrementAndGet();
                    } else {
                        Thread.sleep(1);
                        sharedSales.cancel("취소 사유 " + threadNum);
                        cancelCallCount.incrementAndGet();
                    }
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
        System.out.println("[cancelSales Race Condition 테스트 결과]");
        System.out.println("총 요청 수: " + VUSER_COUNT);
        System.out.println("cancel() 호출 횟수: " + cancelCallCount.get());
        System.out.println("스킵 횟수: " + skipCount.get());
        System.out.println("=".repeat(60));

        if (cancelCallCount.get() > 1) {
            System.out.println("⚠️  RACE CONDITION 발생! cancel()이 " + cancelCallCount.get() + "번 호출됨");
        }

        assertThat(sharedSales.getStatus()).isEqualTo(SalesStatus.CANCELLED);
    }
}
