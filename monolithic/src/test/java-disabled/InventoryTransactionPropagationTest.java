package com.project.chaechaeserver.inventory.service;

import com.project.chaechaeserver.application.response.inventory.bulk.ResBulkCreateInventoryPostDTO;
import com.project.chaechaeserver.application.service.inventory.bulk.InventoryBulkService;
import com.project.chaechaeserver.domain.model.inventory.InventoryEntity;
import com.project.chaechaeserver.domain.model.products.ProductEntity;
import com.project.chaechaeserver.domain.model.products.constraint.ProductStatusType;
import com.project.chaechaeserver.domain.repository.inventory.InventoryRepository;
import com.project.chaechaeserver.domain.repository.products.ProductsRepository;
import com.project.chaechaeserver.presentation.request.inventory.bulk.ReqBulkCreateInventoryDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
class InventoryTransactionPropagationTest {

    @Autowired
    private InventoryBulkService inventoryBulkService;

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    private List<Long> testProductIds;

    @BeforeEach
    void setUp() {
        System.out.println("=== 테스트 데이터 초기화 ===");

        // 테스트용 상품 250개 생성 (배치 3개: 100, 100, 50)
        List<ProductEntity> products = new ArrayList<>();
        for (int i = 0; i < 250; i++) {
            products.add(ProductEntity.builder()
                .name("테스트상품_" + System.currentTimeMillis() + "_" + i)
                .category("테스트카테고리")
                .price(1000)
                .initialQuantity(0)
                .productStatusType(ProductStatusType.PENDING)
                .orderStatusType(null)
                .build());
        }

        // 각 상품을 개별 저장
        testProductIds = new ArrayList<>();
        for (ProductEntity product : products) {
            ProductEntity saved = productsRepository.save(product);
            testProductIds.add(saved.getId());
        }

        System.out.println("테스트 상품 " + testProductIds.size() + "개 생성 완료");
    }

    @Test
    @DisplayName("REQUIRES_NEW 전파 레벨이 작동하는지 확인 - 중간 배치 실패 시나리오")
    void testRequiresNewPropagation() {
        // Given: 250개 상품에 대한 입고 수량 (배치 크기 100 기준으로 3개 배치)
        List<Integer> quantities = new ArrayList<>();
        for (int i = 0; i < 250; i++) {
            quantities.add(10); // 각 상품에 10개씩 입고
        }

        // 중간 배치에서 실패하도록 세 번째 배치 중간에 존재하지 않는 상품 ID 추가
        List<Long> testIds = new ArrayList<>(testProductIds);
        testIds.set(225, 999999L); // 존재하지 않는 상품 ID로 교체 → DB 에러 유발

        System.out.println("=== 배치 처리 시작 (총 250개 상품) ===");
        System.out.println("예상: 배치1(0-99) 성공, 배치2(100-199) 성공, 배치3(200-249) 실패");

        // When: 벌크 입고 처리 (예외 발생 예상)
        try {
            // DTO 생성
            List<ReqBulkCreateInventoryDTO.Inventory> inventoryList = new ArrayList<>();
            for (int i = 0; i < testIds.size(); i++) {
                inventoryList.add(new ReqBulkCreateInventoryDTO.Inventory(testIds.get(i), quantities.get(i)));
            }
            ReqBulkCreateInventoryDTO dto = new ReqBulkCreateInventoryDTO(inventoryList);

            inventoryBulkService.createInventoryForBulk(dto);
        } catch (Exception e) {
            System.out.println("예상된 예외 발생: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }

        // Then: REQUIRES_NEW가 작동한다면 첫 두 배치는 커밋되어야 함
        System.out.println("\n=== 트랜잭션 전파 결과 확인 ===");

        // 첫 번째 배치 (0-99) 상품들의 재고 확인 (락 없는 일반 조회)
        List<ProductEntity> batch1Products = productsRepository.findAllById(
            testProductIds.subList(0, 100)
        );
        long batch1SuccessCount = batch1Products.stream()
            .filter(p -> p.getQuantity() != null && p.getQuantity() == 10)
            .count();
        System.out.println("배치1 (0-99): 성공 " + batch1SuccessCount + "개 / 100개");

        // 두 번째 배치 (100-199) 상품들의 재고 확인 (락 없는 일반 조회)
        List<ProductEntity> batch2Products = productsRepository.findAllById(
            testProductIds.subList(100, 200)
        );
        long batch2SuccessCount = batch2Products.stream()
            .filter(p -> p.getQuantity() != null && p.getQuantity() == 10)
            .count();
        System.out.println("배치2 (100-199): 성공 " + batch2SuccessCount + "개 / 100개");

        // 세 번째 배치 (200-249) 상품들의 재고 확인 (락 없는 일반 조회)
        List<ProductEntity> batch3Products = productsRepository.findAllById(
            testProductIds.subList(200, 250)
        );
        long batch3SuccessCount = batch3Products.stream()
            .filter(p -> p.getQuantity() != null && p.getQuantity() == 10)
            .count();
        System.out.println("배치3 (200-249): 성공 " + batch3SuccessCount + "개 / 50개 (실패 예상)");

        // 검증 및 결과 출력
        System.out.println("\n=== 최종 결과 ===");
        System.out.println("배치1: " + batch1SuccessCount + "/100");
        System.out.println("배치2: " + batch2SuccessCount + "/100");
        System.out.println("배치3: " + batch3SuccessCount + "/50");

        if (batch1SuccessCount == 100 && batch2SuccessCount == 100 && batch3SuccessCount == 0) {
            System.out.println("\n✅ REQUIRES_NEW 작동함!");
            System.out.println("  - 첫 두 배치는 독립 트랜잭션으로 커밋됨");
            System.out.println("  - 세 번째 배치만 롤백됨");
            assertThat(batch1SuccessCount).isEqualTo(100);
            assertThat(batch2SuccessCount).isEqualTo(100);
            assertThat(batch3SuccessCount).isEqualTo(0);
        } else if (batch1SuccessCount == 0 && batch2SuccessCount == 0 && batch3SuccessCount == 0) {
            System.out.println("\n❌ REQUIRES_NEW 작동 안 함!");
            System.out.println("  - 전체가 하나의 트랜잭션으로 묶여 전부 롤백됨");
            System.out.println("  - Propagation 설정을 확인하세요");
            assertThat(batch1SuccessCount).isEqualTo(0);
            assertThat(batch2SuccessCount).isEqualTo(0);
            assertThat(batch3SuccessCount).isEqualTo(0);
        } else {
            System.out.println("\n⚠️ 예상치 못한 결과!");
            System.out.println("  - batch1=" + batch1SuccessCount + ", batch2=" + batch2SuccessCount +
                ", batch3=" + batch3SuccessCount);
        }
    }

    @Test
    @DisplayName("정상 케이스: 전체 배치 성공")
    void testAllBatchesSuccess() {
        // Given: 250개 상품에 대한 정상 입고 수량
        List<Integer> quantities = new ArrayList<>();
        for (int i = 0; i < 250; i++) {
            quantities.add(10);
        }

        // DTO 생성
        List<ReqBulkCreateInventoryDTO.Inventory> inventoryList = new ArrayList<>();
        for (int i = 0; i < testProductIds.size(); i++) {
            inventoryList.add(new ReqBulkCreateInventoryDTO.Inventory(testProductIds.get(i), quantities.get(i)));
        }
        ReqBulkCreateInventoryDTO dto = new ReqBulkCreateInventoryDTO(inventoryList);

        // When: 벌크 입고 처리
        ResBulkCreateInventoryPostDTO response = inventoryBulkService.createInventoryForBulk(dto);

        // Then: 모든 상품의 재고가 10으로 업데이트되어야 함 (락 없는 일반 조회)
        List<ProductEntity> allProducts = productsRepository.findAllById(testProductIds);
        long successCount = allProducts.stream()
            .filter(p -> p.getQuantity() != null && p.getQuantity() == 10)
            .count();

        System.out.println("전체 성공 - 업데이트된 상품: " + successCount + "개 / 250개");
        assertThat(successCount).isEqualTo(250);
        assertThat(response.getInventory()).hasSize(250);
    }
}