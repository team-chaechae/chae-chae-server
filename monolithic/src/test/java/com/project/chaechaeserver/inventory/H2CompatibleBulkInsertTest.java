package com.project.chaechaeserver.inventory;

import com.project.chaechaeserver.domain.model.inventory.InventoryEntity;
import com.project.chaechaeserver.domain.model.inventory.constraint.InventoryChangeType;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * H2 호환 벌크 인서트 테스트
 */
class H2CompatibleBulkInsertTest {

    @Test
    void testBulkInsertWithH2() {
        // H2 인메모리 DB 설정
        DataSource dataSource = createH2DataSource();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        // 테이블 생성 (H2 문법)
        createH2Table(jdbcTemplate);

        // 테스트 데이터
        List<InventoryEntity> entities = new ArrayList<>();
        entities.add(InventoryEntity.builder()
            .productId(1L)
            .quantity(100)
            .changeType(InventoryChangeType.RECEIVE)
            .build());
        entities.add(InventoryEntity.builder()
            .productId(2L)
            .quantity(-50)
            .changeType(InventoryChangeType.SALE)
            .build());
        entities.add(InventoryEntity.builder()
            .productId(3L)
            .quantity(30)
            .changeType(InventoryChangeType.ADJUST)
            .build());
        entities.add(InventoryEntity.builder()
            .productId(4L)
            .quantity(200)
            .changeType(null)  // 기본값 테스트
            .build());

        // H2 호환 벌크 인서트 실행
        int[] results = h2CompatibleBulkInsert(jdbcTemplate, entities);

        System.out.println("✅ H2 벌크 인서트 성공! " + results.length + " 건 삽입");

        // 검증
        List<Map<String, Object>> savedData = jdbcTemplate.queryForList(
            "SELECT product_id, quantity, change_type FROM INVENTORY ORDER BY product_id"
        );

        assertThat(savedData).hasSize(4);

        // 데이터 확인
        for (Map<String, Object> row : savedData) {
            System.out.printf("Product ID: %s, Quantity: %s, Change Type: %s%n",
                row.get("PRODUCT_ID"), row.get("QUANTITY"), row.get("CHANGE_TYPE"));
        }

        // 검증
        assertThat(savedData.get(0).get("CHANGE_TYPE")).isEqualTo("RECEIVE");
        assertThat(savedData.get(1).get("CHANGE_TYPE")).isEqualTo("SALE");
        assertThat(savedData.get(2).get("CHANGE_TYPE")).isEqualTo("ADJUST");
        assertThat(savedData.get(3).get("CHANGE_TYPE")).isEqualTo("RECEIVE"); // null -> 기본값

        System.out.println("✅ 모든 테스트 통과!");
    }

    private DataSource createH2DataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    private void createH2Table(JdbcTemplate jdbcTemplate) {
        // H2용 테이블 생성
        String sql = """
            CREATE TABLE INVENTORY (
                ID BIGINT AUTO_INCREMENT PRIMARY KEY,
                PRODUCT_ID BIGINT NOT NULL,
                QUANTITY INT NOT NULL,
                CHANGE_TYPE VARCHAR(20) DEFAULT 'RECEIVE',
                CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UPDATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
        jdbcTemplate.execute(sql);
        System.out.println("✅ H2 테이블 생성 완료");
    }

    private int[] h2CompatibleBulkInsert(JdbcTemplate jdbcTemplate, List<InventoryEntity> entities) {
        String sql = """
            INSERT INTO INVENTORY (PRODUCT_ID, QUANTITY, CHANGE_TYPE, CREATED_AT, UPDATED_AT)
            VALUES (?, ?, ?, ?, ?)
            """;

        LocalDateTime now = LocalDateTime.now();
        List<Object[]> batchArgs = new ArrayList<>();

        for (InventoryEntity entity : entities) {
            batchArgs.add(new Object[]{
                entity.getProductId(),
                entity.getQuantity(),
                entity.getChangeType() != null ? entity.getChangeType().name() : "RECEIVE",
                Timestamp.valueOf(now),
                Timestamp.valueOf(now)
            });
        }

        return jdbcTemplate.batchUpdate(sql, batchArgs);
    }
}