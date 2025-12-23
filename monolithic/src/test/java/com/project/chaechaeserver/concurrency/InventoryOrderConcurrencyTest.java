package com.project.chaechaeserver.concurrency;

import com.project.chaechaeserver.application.service.inventory.bulk.InventoryBulkService;
import com.project.chaechaeserver.application.service.order.order_customer.OrderCustomerService;
import com.project.chaechaeserver.domain.repository.inventory.InventoryRepository;
import com.project.chaechaeserver.presentation.request.inventory.bulk.ReqBulkCreateInventoryDTO;
import com.project.chaechaeserver.presentation.request.order.ReqOrderCustomerPostCreateDTO;
import com.project.chaechaeserver.presentation.request.order.ReqOrderCustomerPostCreateDTO.Order;
import com.project.chaechaeserver.presentation.request.order.ReqOrderCustomerPostCreateDTO.Order.OrderItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@DisplayName("재고 입고와 주문 동시성 테스트")
class InventoryOrderConcurrencyTest {

    @Autowired
    private InventoryBulkService inventoryBulkService;

    @Autowired
    private OrderCustomerService orderCustomerService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    @DisplayName("재고 입고와 고객 주문이 동시에 발생할 때 데이터 정합성 테스트 (대용량)")
    void testConcurrentInventoryReceiveAndOrder() throws InterruptedException {
        // Given: 테스트 상품 ID
        List<Long> productIds = List.of(1L, 2L, 3L, 4L, 5L);

        // 초기 재고 확인
        Map<Long, Integer> initialStock = inventoryRepository.getCurrentStockMap(productIds);
        System.out.println("=== 초기 재고 ===");
        initialStock.forEach((productId, stock) ->
            System.out.println("상품 " + productId + ": " + stock + "개"));

        // 동시 실행할 작업 수 (총 10000개 인서트)
        int bulkInsertThreads = 100;  // 재고 입고 스레드 100개 (각 50개씩 = 5000개)
        int orderThreads = 1000;       // 고객 주문 스레드 1000개 (각 5개씩 = 5000개)
        int totalThreads = bulkInsertThreads + orderThreads;

        CountDownLatch startLatch = new CountDownLatch(1);  // 모든 스레드 동시 시작
        CountDownLatch endLatch = new CountDownLatch(totalThreads);  // 모든 스레드 완료 대기
        ExecutorService executorService = Executors.newFixedThreadPool(50);  // 스레드 풀 크기 50으로 제한

        AtomicInteger successfulBulkInserts = new AtomicInteger(0);
        AtomicInteger successfulOrders = new AtomicInteger(0);
        AtomicInteger failedOperations = new AtomicInteger(0);

        // When: 재고 입고 작업 (각 상품당 100개씩 입고)
        for (int i = 0; i < bulkInsertThreads; i++) {
            final int threadNum = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();  // 시작 신호 대기

                    ReqBulkCreateInventoryDTO request = ReqBulkCreateInventoryDTO.builder()
                        .inventory(List.of(
                            new ReqBulkCreateInventoryDTO.Inventory(1L, 100),
                            new ReqBulkCreateInventoryDTO.Inventory(2L, 100),
                            new ReqBulkCreateInventoryDTO.Inventory(3L, 100),
                            new ReqBulkCreateInventoryDTO.Inventory(4L, 100),
                            new ReqBulkCreateInventoryDTO.Inventory(5L, 100)
                        ))
                        .build();

                    inventoryBulkService.createInventory(request);
                    successfulBulkInserts.incrementAndGet();
                    System.out.println("[재고 입고 " + threadNum + "] 완료 - 각 상품 +100개");

                } catch (Exception e) {
                    failedOperations.incrementAndGet();
                    System.err.println("[재고 입고 " + threadNum + "] 실패: " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // When: 고객 주문 작업 (각 상품당 10개씩 주문)
        for (int i = 0; i < orderThreads; i++) {
            final int threadNum = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();  // 시작 신호 대기

                    List<OrderItem> orderItems = List.of(
                        OrderItem.builder()
                            .productId(1)
                            .productName("상품1")
                            .quantity(10)
                            .price(1000)
                            .build(),
                        OrderItem.builder()
                            .productId(2)
                            .productName("상품2")
                            .quantity(10)
                            .price(2000)
                            .build(),
                        OrderItem.builder()
                            .productId(3)
                            .productName("상품3")
                            .quantity(10)
                            .price(3000)
                            .build(),
                        OrderItem.builder()
                            .productId(4)
                            .productName("상품4")
                            .quantity(10)
                            .price(4000)
                            .build(),
                        OrderItem.builder()
                            .productId(5)
                            .productName("상품5")
                            .quantity(10)
                            .price(5000)
                            .build()
                    );

                    ReqOrderCustomerPostCreateDTO request = ReqOrderCustomerPostCreateDTO.builder()
                        .order(Order.builder()
                            .customerId(1L)
                            .orderItems(orderItems)
                            .build())
                        .build();

                    orderCustomerService.createOrder(request);
                    successfulOrders.incrementAndGet();
                    System.out.println("[고객 주문 " + threadNum + "] 완료 - 각 상품 -10개");

                } catch (Exception e) {
                    failedOperations.incrementAndGet();
                    System.err.println("[고객 주문 " + threadNum + "] 실패: " + e.getMessage());
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // 모든 스레드 동시 시작
        System.out.println("\n=== 동시성 테스트 시작 ===");
        System.out.println("총 " + totalThreads + "개 작업 실행 (스레드 풀: 50)");
        long startTime = System.currentTimeMillis();
        startLatch.countDown();

        // 모든 스레드 완료 대기 (최대 5분)
        endLatch.await();
        long endTime = System.currentTimeMillis();

        executorService.shutdown();

        // Then: 결과 검증
        System.out.println("\n=== 테스트 결과 ===");
        System.out.println("실행 시간: " + (endTime - startTime) + "ms");
        System.out.println("성공한 재고 입고: " + successfulBulkInserts.get() + "/" + bulkInsertThreads);
        System.out.println("성공한 고객 주문: " + successfulOrders.get() + "/" + orderThreads);
        System.out.println("실패한 작업: " + failedOperations.get());

        // 최종 재고 확인
        Map<Long, Integer> finalStock = inventoryRepository.getCurrentStockMap(productIds);
        System.out.println("\n=== 최종 재고 ===");
        finalStock.forEach((productId, stock) ->
            System.out.println("상품 " + productId + ": " + stock + "개"));

        // 재고 변화량 계산
        System.out.println("\n=== 재고 변화량 ===");
        for (Long productId : productIds) {
            int initial = initialStock.getOrDefault(productId, 0);
            int current = finalStock.getOrDefault(productId, 0);
            int change = current - initial;
            System.out.println("상품 " + productId + ": " + initial + " → " + current + " (변화: " + change + ")");
        }

        // 예상 재고 변화량
        int expectedIncrease = successfulBulkInserts.get() * 100 * 5;  // 재고 입고
        int expectedDecrease = successfulOrders.get() * 10 * 5;        // 고객 주문
        int expectedNetChange = expectedIncrease - expectedDecrease;

        System.out.println("\n=== 예상 재고 변화 ===");
        System.out.println("예상 증가: +" + expectedIncrease + "개 (입고 " + successfulBulkInserts.get() + "회 x 100개 x 5상품)");
        System.out.println("예상 감소: -" + expectedDecrease + "개 (주문 " + successfulOrders.get() + "회 x 10개 x 5상품)");
        System.out.println("예상 순변화: " + expectedNetChange + "개");

        // 검증: 각 상품의 실제 재고 변화가 예상과 일치하는지 확인
        for (Long productId : productIds) {
            int initial = initialStock.getOrDefault(productId, 0);
            int current = finalStock.getOrDefault(productId, 0);
            int actualChange = current - initial;
            int expectedChangePerProduct = (expectedIncrease - expectedDecrease) / 5;

            assertThat(actualChange)
                .withFailMessage(
                    "상품 %d의 재고 변화가 예상과 다릅니다. 예상: %d, 실제: %d",
                    productId, expectedChangePerProduct, actualChange
                )
                .isEqualTo(expectedChangePerProduct);
        }

        System.out.println("\n✅ 동시성 테스트 통과: 데이터 정합성 검증 완료");
    }
}