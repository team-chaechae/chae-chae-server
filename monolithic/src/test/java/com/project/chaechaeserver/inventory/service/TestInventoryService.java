//package com.project.chaechaeserver.inventory.service;
//
//import com.project.chaechaeserver.application.response.inventory.ResSingleCreateInventoryPostDTO;
//import com.project.chaechaeserver.application.service.inventory.InventoryService;
//import com.project.chaechaeserver.presentation.request.inventory.ReqCreateInventoryDTO;
//import com.project.chaechaeserver.presentation.request.inventory.ReqSaleSingleInventoryDTO;
//import com.project.chaechaeserver.presentation.request.inventory.ReqBulkCreateInventoryDTO;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.concurrent.atomic.AtomicLong;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//
//@SpringBootTest
//public class TestInventoryService {
//
//    @Autowired
//    InventoryService inventoryService;
//
//    @Test
//    @DisplayName("동일 상품 재고 경쟁 조건 테스트")
//    void testRaceConditionOnSameProduct() throws InterruptedException {
//        // given
//        int concurrentOperations = 2000;
//        int threadPoolSize = 200;
//
//        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
//        CountDownLatch countDownLatch = new CountDownLatch(concurrentOperations * 2);
//
//        AtomicInteger successCount = new AtomicInteger();
//        AtomicInteger failCount = new AtomicInteger();
//
//        // 동시에 재고 증가 작업 실행
//        for (int i = 0; i < concurrentOperations; i++) {
//            executorService.submit(() -> {
//                try {
//                    ReqCreateInventoryDTO addRequest = ReqCreateInventoryDTO.builder()
//                        .inventory(ReqCreateInventoryDTO.Inventory.builder()
//                            .productId(1L)
//                            .quantity(5)
//                            .build())
//                        .build();
//
//                    inventoryService.addInventory(addRequest);
//                    successCount.incrementAndGet();
//                } catch (Exception e) {
//                    failCount.incrementAndGet();
//                    System.err.println("Add inventory failed: " + e.getMessage());
//                } finally {
//                    countDownLatch.countDown();
//                }
//            });
//        }
//
//        // 동시에 재고 감소 작업 실행
//        for (int i = 0; i < concurrentOperations; i++) {
//            executorService.submit(() -> {
//                try {
//                    ReqSaleSingleInventoryDTO decreaseRequest = ReqSaleSingleInventoryDTO.builder()
//                        .inventory(ReqSaleSingleInventoryDTO.Inventory.builder()
//                            .productId(1L)
//                            .quantity(3)
//                            .build())
//                        .build();
//
//                    inventoryService.decreaseInventory(decreaseRequest);
//                    successCount.incrementAndGet();
//                } catch (Exception e) {
//                    failCount.incrementAndGet();
//                    System.err.println("Decrease inventory failed: " + e.getMessage());
//                } finally {
//                    countDownLatch.countDown();
//                }
//            });
//        }
//
//        // 모든 스레드 완료 대기
//        countDownLatch.await();
//        executorService.shutdown();
//
//        // 결과 출력
//        System.out.println("=== 경쟁 조건 테스트 결과 ===");
//        System.out.println("성공한 작업 수: " + successCount.get());
//        System.out.println("실패한 작업 수: " + failCount.get());
//        System.out.println("총 작업 수: " + (successCount.get() + failCount.get()));
//    }
//
//    @Test
//    @DisplayName("다중 상품 동시성 테스트 - 1번, 2번 상품 각 100명씩 1000번")
//    void testMultiProductConcurrency() throws InterruptedException {
//        // given
//        int usersPerProduct = 20;
//        int operationsPerProduct = 100;
//        int totalThreads = usersPerProduct * 2; // 200개 스레드
//        int totalOperations = operationsPerProduct * 2; // 2000개 작업
//
//        ExecutorService executorService = Executors.newFixedThreadPool(totalThreads);
//        CountDownLatch startLatch = new CountDownLatch(1);
//        CountDownLatch completeLatch = new CountDownLatch(totalOperations);
//
//        // 상품별 성공/실패 카운트
//        AtomicInteger product1AddSuccess = new AtomicInteger();
//        AtomicInteger product1DecreaseSuccess = new AtomicInteger();
//        AtomicInteger product1Fail = new AtomicInteger();
//
//        AtomicInteger product2AddSuccess = new AtomicInteger();
//        AtomicInteger product2DecreaseSuccess = new AtomicInteger();
//        AtomicInteger product2Fail = new AtomicInteger();
//
//        // 시간 측정
//        AtomicLong startTime = new AtomicLong();
//        AtomicLong endTime = new AtomicLong();
//
//        System.out.println("=== 다중 상품 동시성 테스트 시작 ===");
//        System.out.println("총 스레드 수: " + totalThreads);
//        System.out.println("총 작업 수: " + totalOperations);
//        System.out.println("상품 1: " + operationsPerProduct + "번 작업 (스레드 1-100)");
//        System.out.println("상품 2: " + operationsPerProduct + "번 작업 (스레드 101-200)");
//
//        // 상품 1번 작업 (1000번)
//        for (int i = 0; i < operationsPerProduct; i++) {
//            int operationIndex = i;
//            executorService.submit(() -> {
//                try {
//                    startLatch.await(); // 동시 시작 대기
//
//                    if (startTime.get() == 0) {
//                        startTime.compareAndSet(0, System.currentTimeMillis());
//                    }
//
//                    ReqCreateInventoryDTO request = ReqCreateInventoryDTO.builder()
//                        .inventory(ReqCreateInventoryDTO.Inventory.builder()
//                            .productId(1L) // 상품 1번
//                            .quantity(5)
//                            .build())
//                        .build();
//                    ReqSaleSingleInventoryDTO req = ReqSaleSingleInventoryDTO.builder()
//                        .inventory(ReqSaleSingleInventoryDTO.Inventory.builder()
//                            .productId(1L) // 상품 1번
//                            .quantity(5)
//                            .build())
//                        .build();
//
//                    // 짝수는 증가, 홀수는 감소
//                    if (operationIndex % 2 == 0) {
//                        inventoryService.addInventory(request);
//                        product1AddSuccess.incrementAndGet();
//                    } else {
//                        inventoryService.decreaseInventory(req);
//                        product1DecreaseSuccess.incrementAndGet();
//                    }
//
//                } catch (Exception e) {
//                    product1Fail.incrementAndGet();
//                    System.err.println("[상품1] 실패: " + e.getMessage());
//                } finally {
//                    endTime.set(System.currentTimeMillis());
//                    completeLatch.countDown();
//                }
//            });
//        }
//
//        // 상품 2번 작업 (1000번)
//        for (int i = 0; i < operationsPerProduct; i++) {
//            int operationIndex = i;
//            executorService.submit(() -> {
//                try {
//                    startLatch.await(); // 동시 시작 대기
//
//                    if (startTime.get() == 0) {
//                        startTime.compareAndSet(0, System.currentTimeMillis());
//                    }
//
//                    ReqCreateInventoryDTO request = ReqCreateInventoryDTO.builder()
//                        .inventory(ReqCreateInventoryDTO.Inventory.builder()
//                            .productId(2L) // 상품 2번
//                            .quantity(5)
//                            .build())
//                        .build();
//
//                    ReqSaleSingleInventoryDTO req = ReqSaleSingleInventoryDTO.builder()
//                        .inventory(ReqSaleSingleInventoryDTO.Inventory.builder()
//                            .productId(2L) // 상품 2번
//                            .quantity(5)
//                            .build())
//                        .build();
//                    // 짝수는 증가, 홀수는 감소
//                    if (operationIndex % 2 == 0) {
//                        inventoryService.addInventory(request);
//                        product2AddSuccess.incrementAndGet();
//                    } else {
//                        inventoryService.decreaseInventory(req);
//                        product2DecreaseSuccess.incrementAndGet();
//                    }
//
//                } catch (Exception e) {
//                    product2Fail.incrementAndGet();
//                    System.err.println("[상품2] 실패: " + e.getMessage());
//                } finally {
//                    endTime.set(System.currentTimeMillis());
//                    completeLatch.countDown();
//                }
//            });
//        }
//
//        System.out.println("모든 스레드 준비 완료. 동시 실행 시작!");
//
//        // 모든 스레드 동시 시작
//        startLatch.countDown();
//
//        // 완료 대기 (타임아웃 60초)
//        boolean completed = completeLatch.await(60, TimeUnit.SECONDS);
//        executorService.shutdown();
//
//        long executionTime = endTime.get() - startTime.get();
//
//        // 결과 출력
//        System.out.println("\n=== 다중 상품 동시성 테스트 결과 ===");
//        System.out.println("완료 여부: " + (completed ? "정상 완료" : "타임아웃"));
//        System.out.println("총 실행 시간: " + executionTime + "ms");
//
//        // 상품 1번 결과
//        int product1Total = product1AddSuccess.get() + product1DecreaseSuccess.get();
//        System.out.println("\n[상품 1번 결과]");
//        System.out.println("재고 증가 성공: " + product1AddSuccess.get());
//        System.out.println("재고 감소 성공: " + product1DecreaseSuccess.get());
//        System.out.println("실패: " + product1Fail.get());
//        System.out.println("성공 총계: " + product1Total);
//
//        // 상품 2번 결과
//        int product2Total = product2AddSuccess.get() + product2DecreaseSuccess.get();
//        System.out.println("\n[상품 2번 결과]");
//        System.out.println("재고 증가 성공: " + product2AddSuccess.get());
//        System.out.println("재고 감소 성공: " + product2DecreaseSuccess.get());
//        System.out.println("실패: " + product2Fail.get());
//        System.out.println("성공 총계: " + product2Total);
//
//        // 전체 결과
//        int totalSuccess = product1Total + product2Total;
//        int totalFail = product1Fail.get() + product2Fail.get();
//
//        System.out.println("\n[전체 결과]");
//        System.out.println("총 성공: " + totalSuccess);
//        System.out.println("총 실패: " + totalFail);
//        System.out.println("총 실행: " + (totalSuccess + totalFail));
//
//        if (completed && totalSuccess > 0) {
//            double tps = (totalSuccess * 1000.0) / executionTime;
//            System.out.println("처리량: " + String.format("%.1f", tps) + " TPS");
//
//            // 이전 단일 상품 테스트와 비교
//            System.out.println("\n[성능 비교]");
//            System.out.println("이전 (단일 상품 4000건): 266.7 TPS");
//            System.out.println("현재 (다중 상품 2000건): " + String.format("%.1f", tps) + " TPS");
//
//            if (tps > 266.7) {
//                System.out.println("✅ 다중 상품으로 인한 성능 향상 확인!");
//            } else {
//                System.out.println("📊 단일 상품 대비 TPS 변화 확인");
//            }
//        }
//
//        // 병렬 처리 효과 분석
//        if (completed) {
//            System.out.println("\n[병렬 처리 분석]");
//            System.out.println("상품별 평균 처리 시간: " + (executionTime / 2.0) + "ms");
//            System.out.println("락 경합 분산 효과: " + (product1Total > 0 && product2Total > 0 ? "성공" : "실패"));
//        }
//    }
//
//}
