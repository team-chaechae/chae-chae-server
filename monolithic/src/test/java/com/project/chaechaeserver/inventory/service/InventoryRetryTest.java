package com.project.chaechaeserver.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.project.chaechaeserver.application.service.inventory.bulk.InventoryBulkService;
import com.project.chaechaeserver.domain.model.products.ProductEntity;
import com.project.chaechaeserver.domain.repository.inventory.InventoryRepository;
import com.project.chaechaeserver.domain.repository.products.ProductsRepository;
import com.project.chaechaeserver.presentation.request.inventory.bulk.ReqBulkCreateInventoryDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class InventoryRetryTest {

    @Autowired
    private InventoryBulkService inventoryBulkService;

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    private List<Long> testProductIds;

    @BeforeEach
    void setUp() {
        // 기존 DB 상품 ID (1~23) 사용
        testProductIds = IntStream.rangeClosed(1, 23)
            .mapToObj(Long::valueOf)
            .toList();
        System.out.println("기존 DB 상품을 사용하여 테스트 준비 완료");
    }

    @Test
    @DisplayName("동시성 환경에서 Retry 메커니즘이 Lock 경합을 해결한다")
    void testRetryOnConcurrency() throws Exception {
        // Given: 10개 스레드가 동시에 같은 23개 상품에 대해 입고 요청
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // 각 상품의 기존 재고 조회
        List<ProductEntity> beforeProducts = productsRepository.findAllById(testProductIds);
        java.util.Map<Long, Integer> beforeQuantities = beforeProducts.stream()
            .collect(java.util.stream.Collectors.toMap(
                ProductEntity::getId,
                p -> p.getQuantity() != null ? p.getQuantity() : 0
            ));

        // When: 동시에 배치 입고 요청
        for (int i = 0; i < threadCount; i++) {
            int threadNum = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    latch.countDown();
                    latch.await(); // 모든 스레드가 준비될 때까지 대기

                    // 23개 상품에 대해 각 10개씩 입고
                    List<ReqBulkCreateInventoryDTO.Inventory> inventoryList = new ArrayList<>();
                    for (Long productId : testProductIds) {
                        inventoryList.add(
                            ReqBulkCreateInventoryDTO.Inventory.builder()
                                .productId(productId)
                                .quantity(10)
                                .build()
                        );
                    }

                    ReqBulkCreateInventoryDTO dto = ReqBulkCreateInventoryDTO.builder()
                        .inventory(inventoryList)
                        .build();

                    inventoryBulkService.createInventoryForBulk(dto);
                    successCount.incrementAndGet();
                    System.out.println("Thread-" + threadNum + " 성공");

                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    System.err.println("Thread-" + threadNum + " 실패: " + e.getMessage());
                    e.printStackTrace();
                }
            }, executorService);

            futures.add(future);
        }

        // 모든 작업 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executorService.shutdown();

        // Then: Retry 덕분에 대부분 성공해야 함
        System.out.println("=== 결과 ===");
        System.out.println("성공: " + successCount.get());
        System.out.println("실패: " + failureCount.get());

        // 최소 80% 이상 성공 (Retry로 인해 Lock 경합 해결)
        assertThat(successCount.get()).isGreaterThanOrEqualTo(8);

        // 각 상품의 최종 재고 확인
        List<ProductEntity> afterProducts = productsRepository.findAllById(testProductIds);
        for (ProductEntity product : afterProducts) {
            int beforeQuantity = beforeQuantities.get(product.getId());
            int expectedIncrease = successCount.get() * 10; // 성공한 스레드 수 * 10개
            int expectedQuantity = beforeQuantity + expectedIncrease;

            System.out.println("Product ID " + product.getId() + ": "
                + beforeQuantity + " → " + product.getQuantity()
                + " (expected: " + expectedQuantity + ")");

            assertThat(product.getQuantity()).isEqualTo(expectedQuantity);
        }
    }

    @Test
    @DisplayName("Deadlock 발생 시 Retry로 재시도하여 성공한다")
    void testRetryOnDeadlock() throws Exception {
        // Given: 2개의 스레드가 서로 다른 순서로 같은 상품들에 접근
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger(0);

        // 상품 2개만 사용
        List<Long> product2Ids = testProductIds.subList(0, 2);

        // 기존 재고 조회
        List<ProductEntity> beforeProducts = productsRepository.findAllById(product2Ids);
        java.util.Map<Long, Integer> beforeQuantities = beforeProducts.stream()
            .collect(java.util.stream.Collectors.toMap(
                ProductEntity::getId,
                p -> p.getQuantity() != null ? p.getQuantity() : 0
            ));

        // Thread-1: [Product-1, Product-2] 순서로 처리
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
            try {
                latch.countDown();
                latch.await();

                List<ReqBulkCreateInventoryDTO.Inventory> inventoryList = new ArrayList<>();
                for (Long productId : product2Ids) { // 정방향
                    inventoryList.add(
                        ReqBulkCreateInventoryDTO.Inventory.builder()
                            .productId(productId)
                            .quantity(10)
                            .build()
                    );
                }

                ReqBulkCreateInventoryDTO dto = ReqBulkCreateInventoryDTO.builder()
                    .inventory(inventoryList)
                    .build();

                inventoryBulkService.createInventoryForBulk(dto);
                successCount.incrementAndGet();
                System.out.println("Thread-1 성공");

            } catch (Exception e) {
                System.err.println("Thread-1 실패: " + e.getMessage());
                e.printStackTrace();
            }
        }, executorService);

        // Thread-2: [Product-2, Product-1] 순서로 처리 (역방향 - Deadlock 유발 가능)
        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
            try {
                latch.countDown();
                latch.await();

                List<ReqBulkCreateInventoryDTO.Inventory> inventoryList = new ArrayList<>();
                List<Long> reversedIds = new ArrayList<>(product2Ids);
                java.util.Collections.reverse(reversedIds); // 역방향

                for (Long productId : reversedIds) {
                    inventoryList.add(
                        ReqBulkCreateInventoryDTO.Inventory.builder()
                            .productId(productId)
                            .quantity(10)
                            .build()
                    );
                }

                ReqBulkCreateInventoryDTO dto = ReqBulkCreateInventoryDTO.builder()
                    .inventory(inventoryList)
                    .build();

                inventoryBulkService.createInventoryForBulk(dto);
                successCount.incrementAndGet();
                System.out.println("Thread-2 성공");

            } catch (Exception e) {
                System.err.println("Thread-2 실패: " + e.getMessage());
                e.printStackTrace();
            }
        }, executorService);

        // 모든 작업 완료 대기
        CompletableFuture.allOf(future1, future2).join();
        executorService.shutdown();

        // Then: Deadlock이 발생하더라도 Retry로 해결되어야 함
        System.out.println("=== Deadlock 테스트 결과 ===");
        System.out.println("성공: " + successCount.get());

        // 두 스레드 모두 성공해야 함 (Retry 덕분)
        // 단, findAllByIdInForWrite로 한번에 락을 획득하므로 Deadlock은 발생하지 않을 수도 있음
        assertThat(successCount.get()).isEqualTo(2);

        // 각 상품은 20개씩 증가해야 함
        List<ProductEntity> afterProducts = productsRepository.findAllById(product2Ids);
        for (ProductEntity product : afterProducts) {
            int beforeQuantity = beforeQuantities.get(product.getId());
            int expectedQuantity = beforeQuantity + 20;

            System.out.println("Product ID " + product.getId() + ": "
                + beforeQuantity + " → " + product.getQuantity()
                + " (expected: " + expectedQuantity + ")");

            assertThat(product.getQuantity()).isEqualTo(expectedQuantity);
        }
    }
}
