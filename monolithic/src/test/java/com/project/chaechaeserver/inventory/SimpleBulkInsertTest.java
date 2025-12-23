package com.project.chaechaeserver.inventory;

import com.project.chaechaeserver.domain.model.inventory.InventoryEntity;
import com.project.chaechaeserver.domain.model.inventory.constraint.InventoryChangeType;
import com.project.chaechaeserver.infrastructure.inventory.JdbcInventoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 간단한 벌크 인서트 테스트 - Spring Context 없이 실행
 */
class SimpleBulkInsertTest {

    @Test
    void testBulkInsertWithChangeType() {
        try {
            // H2 인메모리 DB 설정
            DataSource dataSource = createDataSource();
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

            // 테이블 생성 (벌크 인서트 전에 실행)
            createTable(jdbcTemplate);

            // 테이블 생성 확인
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'INVENTORY'", Integer.class);
            System.out.println("Table created? Count: " + count);

            // JdbcInventoryRepository 생성
            JdbcInventoryRepository repository = new JdbcInventoryRepository(jdbcTemplate);

            // 테스트 데이터 생성
            List<InventoryEntity> inventoryEntities = new ArrayList<>();

            // RECEIVE 타입
            inventoryEntities.add(InventoryEntity.builder()
                .productId(1L)
                .quantity(100)
                .changeType(InventoryChangeType.RECEIVE)
                .build());

            // SALE 타입
            inventoryEntities.add(InventoryEntity.builder()
                .productId(2L)
                .quantity(-50)
                .changeType(InventoryChangeType.SALE)
                .build());

            // ADJUST 타입
            inventoryEntities.add(InventoryEntity.builder()
                .productId(3L)
                .quantity(30)
                .changeType(InventoryChangeType.ADJUST)
                .build());

            // null 타입 (기본값 RECEIVE로 저장되어야 함)
            inventoryEntities.add(InventoryEntity.builder()
                .productId(4L)
                .quantity(200)
                .changeType(null)
                .build());

            // 벌크 인서트 실행
            int result = repository.bulkInsert(inventoryEntities);

            // 검증
            assertThat(result).isEqualTo(4);

            // 저장된 데이터 확인
            List<Map<String, Object>> savedData = jdbcTemplate.queryForList(
                "SELECT product_id, quantity, change_type FROM inventory ORDER BY product_id"
            );

            // RECEIVE 타입 검증
            assertThat(savedData.get(0).get("change_type")).isEqualTo("RECEIVE");

            // SALE 타입 검증
            assertThat(savedData.get(1).get("change_type")).isEqualTo("SALE");

            // ADJUST 타입 검증
            assertThat(savedData.get(2).get("change_type")).isEqualTo("ADJUST");

            // null -> RECEIVE 기본값 검증
            assertThat(savedData.get(3).get("change_type")).isEqualTo("RECEIVE");

            System.out.println("✅ 벌크 인서트 change_type 필드 테스트 성공!");
            System.out.println("✅ null change_type이 RECEIVE로 기본값 설정됨!");

            for (Map<String, Object> row : savedData) {
                System.out.println(String.format("  Product ID: %s, Quantity: %s, Change Type: %s",
                    row.get("product_id"), row.get("quantity"), row.get("change_type")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    private DataSource createDataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:testdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    private void createTable(JdbcTemplate jdbcTemplate) {
        String createTableSql = """
            CREATE TABLE inventory (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                product_id BIGINT NOT NULL,
                quantity INT NOT NULL,
                change_type VARCHAR(20) NOT NULL DEFAULT 'RECEIVE',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
        jdbcTemplate.execute(createTableSql);
    }
}