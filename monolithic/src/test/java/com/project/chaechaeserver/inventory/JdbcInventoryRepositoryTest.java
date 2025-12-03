package com.project.chaechaeserver.inventory;

import com.project.chaechaeserver.domain.model.inventory.InventoryEntity;
import com.project.chaechaeserver.domain.model.inventory.constraint.InventoryChangeType;
import com.project.chaechaeserver.infrastructure.inventory.JdbcInventoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JdbcInventoryRepositoryTest {

    @Autowired
    private JdbcInventoryRepository jdbcInventoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("벌크 인서트 시 change_type 필드가 정상적으로 저장되어야 한다")
    void bulkInsert_WithChangeType_ShouldSaveSuccessfully() {
        // given
        List<InventoryEntity> inventoryEntities = new ArrayList<>();

        // RECEIVE 타입 추가
        inventoryEntities.add(InventoryEntity.builder()
            .productId(1L)
            .quantity(100)
            .changeType(InventoryChangeType.RECEIVE)
            .build());

        // SALE 타입 추가
        inventoryEntities.add(InventoryEntity.builder()
            .productId(2L)
            .quantity(-50)
            .changeType(InventoryChangeType.SALE)
            .build());

        // ADJUST 타입 추가
        inventoryEntities.add(InventoryEntity.builder()
            .productId(3L)
            .quantity(30)
            .changeType(InventoryChangeType.ADJUST)
            .build());

        // when
        int result = jdbcInventoryRepository.bulkInsert(inventoryEntities);

        // then
        assertThat(result).isEqualTo(3);

        // 저장된 데이터 확인
        List<Map<String, Object>> savedData = jdbcTemplate.queryForList(
            "SELECT product_id, quantity, change_type FROM inventory ORDER BY product_id"
        );

        assertThat(savedData).hasSize(3);

        // 첫 번째 레코드 검증 (RECEIVE)
        assertThat(savedData.get(0).get("product_id")).isEqualTo(1L);
        assertThat(savedData.get(0).get("quantity")).isEqualTo(100);
        assertThat(savedData.get(0).get("change_type")).isEqualTo("RECEIVE");

        // 두 번째 레코드 검증 (SALE)
        assertThat(savedData.get(1).get("product_id")).isEqualTo(2L);
        assertThat(savedData.get(1).get("quantity")).isEqualTo(-50);
        assertThat(savedData.get(1).get("change_type")).isEqualTo("SALE");

        // 세 번째 레코드 검증 (ADJUST)
        assertThat(savedData.get(2).get("product_id")).isEqualTo(3L);
        assertThat(savedData.get(2).get("quantity")).isEqualTo(30);
        assertThat(savedData.get(2).get("change_type")).isEqualTo("ADJUST");
    }

    @Test
    @DisplayName("changeType이 null인 경우 기본값 RECEIVE로 저장되어야 한다")
    void bulkInsert_WithNullChangeType_ShouldUseDefaultReceive() {
        // given
        List<InventoryEntity> inventoryEntities = new ArrayList<>();

        // changeType이 null인 엔티티 (실제로는 Builder에서 설정되지 않은 경우)
        InventoryEntity entityWithNullType = InventoryEntity.builder()
            .productId(4L)
            .quantity(200)
            .changeType(null)  // 명시적으로 null 설정
            .build();
        inventoryEntities.add(entityWithNullType);

        // when
        int result = jdbcInventoryRepository.bulkInsert(inventoryEntities);

        // then
        assertThat(result).isEqualTo(1);

        Map<String, Object> savedData = jdbcTemplate.queryForMap(
            "SELECT product_id, quantity, change_type FROM inventory WHERE product_id = 4"
        );

        assertThat(savedData.get("product_id")).isEqualTo(4L);
        assertThat(savedData.get("quantity")).isEqualTo(200);
        assertThat(savedData.get("change_type")).isEqualTo("RECEIVE"); // 기본값 확인
    }

    @Test
    @DisplayName("대량 데이터 벌크 인서트 성능 테스트")
    void bulkInsert_LargeDataSet_PerformanceTest() {
        // given
        int dataSize = 1000;
        List<InventoryEntity> inventoryEntities = new ArrayList<>();

        for (int i = 1; i <= dataSize; i++) {
            InventoryChangeType changeType = i % 3 == 0 ? InventoryChangeType.RECEIVE :
                                            i % 3 == 1 ? InventoryChangeType.SALE :
                                            InventoryChangeType.ADJUST;

            inventoryEntities.add(InventoryEntity.builder()
                .productId((long) i)
                .quantity(i * 10)
                .changeType(changeType)
                .build());
        }

        long startTime = System.currentTimeMillis();

        // when
        int result = jdbcInventoryRepository.bulkInsert(inventoryEntities);

        long endTime = System.currentTimeMillis();

        // then
        assertThat(result).isEqualTo(dataSize);

        long executionTime = endTime - startTime;
        System.out.println("Bulk insert of " + dataSize + " records took " + executionTime + "ms");

        // 1000건 인서트가 1초 이내에 완료되어야 함
        assertThat(executionTime).isLessThan(1000);

        // 데이터 개수 확인
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM inventory", Integer.class
        );
        assertThat(count).isEqualTo(dataSize);
    }

    @Test
    @DisplayName("빈 리스트를 전달하면 0을 반환해야 한다")
    void bulkInsert_EmptyList_ShouldReturnZero() {
        // given
        List<InventoryEntity> emptyList = new ArrayList<>();

        // when
        int result = jdbcInventoryRepository.bulkInsert(emptyList);

        // then
        assertThat(result).isEqualTo(0);
    }

    @Test
    @DisplayName("null 리스트를 전달하면 0을 반환해야 한다")
    void bulkInsert_NullList_ShouldReturnZero() {
        // when
        int result = jdbcInventoryRepository.bulkInsert(null);

        // then
        assertThat(result).isEqualTo(0);
    }
}