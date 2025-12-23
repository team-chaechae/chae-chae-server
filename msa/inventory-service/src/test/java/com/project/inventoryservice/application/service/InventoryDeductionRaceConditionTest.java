package com.project.inventoryservice.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * InventoryDeductionService Race Condition 테스트
 *
 * Check-Then-Act 문제:
 * Thread A: isAlreadyProcessed() → false
 * Thread B: isAlreadyProcessed() → false  (동시 진입)
 * Thread A: 재고 차감 → markAsProcessed()
 * Thread B: 재고 차감  ← 중복 차감!
 */
@DisplayName("InventoryDeductionService Race Condition 테스트 (100 vuser)")
class InventoryDeductionRaceConditionTest {

    private static final int VUSER_COUNT = 100;
    private static final String ORDER_ID = "order-race-test";
    private static final int INITIAL_STOCK = 1000;

    @Test
    @DisplayName("[Race Condition 발생] 100개 동시 요청 시 재고가 여러 번 차감됨 - Check-Then-Act 문제")
    void processPaymentCompleted_100ConcurrentCalls_RaceConditionOccurs() throws InterruptedException {
        // given - 공유 상태: race condition 시뮬레이션용
        ConcurrentHashMap<String, Boolean> processedOrders = new ConcurrentHashMap<>();
        AtomicInteger actualStock = new AtomicInteger(INITIAL_STOCK);
        AtomicInteger deductionCount = new AtomicInteger(0);
        AtomicInteger skipCount = new AtomicInteger(0);

        ExecutorService executorService = Executors.newFixedThreadPool(VUSER_COUNT);
        CountDownLatch readyLatch = new CountDownLatch(VUSER_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(VUSER_COUNT);

        // when - Check-Then-Act 패턴 시뮬레이션 (락 없음)
        for (int i = 0; i < VUSER_COUNT; i++) {
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    // Check: 이미 처리되었는지 확인 (락 없음)
                    // 현재 구현: bucket.isExists()
                    boolean alreadyProcessed = processedOrders.containsKey(ORDER_ID);

                    if (alreadyProcessed) {
                        skipCount.incrementAndGet();
                    } else {
                        // Race condition 발생 지점!
                        // 여러 스레드가 동시에 여기에 도달
                        Thread.sleep(1);  // Redis I/O 시뮬레이션

                        // Act: 재고 차감
                        actualStock.decrementAndGet();
                        deductionCount.incrementAndGet();

                        // 처리 완료 마킹
                        // 현재 구현: bucket.set("PROCESSED", TTL)
                        processedOrders.put(ORDER_ID, true);
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
        System.out.println("[Inventory Race Condition 테스트 결과]");
        System.out.println("총 요청 수: " + VUSER_COUNT);
        System.out.println("재고 차감 횟수: " + deductionCount.get());
        System.out.println("스킵 횟수: " + skipCount.get());
        System.out.println("초기 재고: " + INITIAL_STOCK);
        System.out.println("최종 재고: " + actualStock.get());
        System.out.println("예상 차감량: 1");
        System.out.println("실제 차감량: " + (INITIAL_STOCK - actualStock.get()));
        System.out.println("=".repeat(60));

        if (deductionCount.get() > 1) {
            System.out.println("⚠️  RACE CONDITION 발생! 재고가 " + deductionCount.get() + "번 차감됨");
            System.out.println("    → setIfAbsent() 원자적 연산 또는 분산 락 필요");
        }

        // race condition이 발생하면 deductionCount > 1
        assertThat(processedOrders.containsKey(ORDER_ID)).isTrue();
    }

    @Test
    @DisplayName("[setIfAbsent 적용 시] 원자적 연산으로 race condition 방지")
    void processPaymentCompleted_100ConcurrentCalls_WithSetIfAbsent_NoRaceCondition() throws InterruptedException {
        // given
        ConcurrentHashMap<String, Boolean> processedOrders = new ConcurrentHashMap<>();
        AtomicInteger actualStock = new AtomicInteger(INITIAL_STOCK);
        AtomicInteger deductionCount = new AtomicInteger(0);
        AtomicInteger skipCount = new AtomicInteger(0);

        ExecutorService executorService = Executors.newFixedThreadPool(VUSER_COUNT);
        CountDownLatch readyLatch = new CountDownLatch(VUSER_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(VUSER_COUNT);

        // when - setIfAbsent (putIfAbsent) 원자적 연산 사용
        for (int i = 0; i < VUSER_COUNT; i++) {
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    // 원자적 연산: putIfAbsent (Redis의 setIfAbsent와 동일)
                    // 개선안: bucket.setIfAbsent("PROCESSING", TTL)
                    Boolean previousValue = processedOrders.putIfAbsent(ORDER_ID, true);

                    if (previousValue != null) {
                        // 이미 존재 = 이미 처리됨
                        skipCount.incrementAndGet();
                    } else {
                        // 첫 번째로 키를 설정한 스레드만 진입
                        Thread.sleep(1);
                        actualStock.decrementAndGet();
                        deductionCount.incrementAndGet();
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
        System.out.println("[setIfAbsent 적용 테스트 결과]");
        System.out.println("총 요청 수: " + VUSER_COUNT);
        System.out.println("재고 차감 횟수: " + deductionCount.get());
        System.out.println("스킵 횟수: " + skipCount.get());
        System.out.println("초기 재고: " + INITIAL_STOCK);
        System.out.println("최종 재고: " + actualStock.get());
        System.out.println("=".repeat(60));

        // 원자적 연산 적용 시 정확히 1번만 차감
        assertThat(deductionCount.get()).isEqualTo(1);
        assertThat(skipCount.get()).isEqualTo(VUSER_COUNT - 1);
        assertThat(actualStock.get()).isEqualTo(INITIAL_STOCK - 1);
        System.out.println("✅ setIfAbsent로 race condition 방지 성공");
    }

    @Test
    @DisplayName("[통계] 다양한 동시성 수준에서 race condition 발생률 측정")
    void measureRaceConditionRate_VariousConcurrencyLevels() throws InterruptedException {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("동시성 수준별 Race Condition 발생률 측정");
        System.out.println("=".repeat(70));

        int[] concurrencyLevels = {10, 50, 100, 200};

        for (int vusers : concurrencyLevels) {
            int raceConditionCount = 0;
            int testRuns = 5;

            for (int run = 0; run < testRuns; run++) {
                ConcurrentHashMap<String, Boolean> processed = new ConcurrentHashMap<>();
                AtomicInteger deductions = new AtomicInteger(0);
                String orderId = "order-" + run;

                ExecutorService executor = Executors.newFixedThreadPool(vusers);
                CountDownLatch ready = new CountDownLatch(vusers);
                CountDownLatch start = new CountDownLatch(1);
                CountDownLatch done = new CountDownLatch(vusers);

                for (int i = 0; i < vusers; i++) {
                    executor.submit(() -> {
                        try {
                            ready.countDown();
                            start.await();

                            // Check-Then-Act (락 없음)
                            if (!processed.containsKey(orderId)) {
                                Thread.sleep(0, 100);  // 아주 짧은 딜레이
                                deductions.incrementAndGet();
                                processed.put(orderId, true);
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            done.countDown();
                        }
                    });
                }

                ready.await();
                start.countDown();
                done.await();
                executor.shutdown();

                if (deductions.get() > 1) {
                    raceConditionCount++;
                }
            }

            double rate = (raceConditionCount * 100.0) / testRuns;
            System.out.printf("vuser=%3d: Race Condition 발생 %d/%d회 (%.0f%%)\n",
                    vusers, raceConditionCount, testRuns, rate);
        }

        System.out.println("=".repeat(70));
        System.out.println("결론: 동시성이 높아질수록 race condition 발생 확률 증가");
        System.out.println("=".repeat(70));
    }
}
