package com.project.chaechaeserver;

import com.project.chaechaeserver.domain.model.inventory.InventoryEntity;
import com.project.chaechaeserver.domain.model.inventory.constraint.InventoryChangeType;
import com.project.chaechaeserver.infrastructure.inventory.JdbcInventoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SpringBootApplication
@Profile("test-bulk")
public class TestBulkInsert {

    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "dev,test-bulk");
        SpringApplication.run(TestBulkInsert.class, args);
    }

    @Bean
    @Profile("test-bulk")
    public CommandLineRunner testBulkInsert(
            JdbcInventoryRepository jdbcInventoryRepository,
            JdbcTemplate jdbcTemplate) {
        return args -> {
            System.out.println("\n========== BULK INSERT TEST START ==========\n");

            // Test 1: 벌크 인서트 시 change_type 필드가 정상적으로 저장되는지 테스트
            System.out.println("Test 1: Testing bulk insert with change_type field");

            List<InventoryEntity> inventoryEntities = new ArrayList<>();

            // RECEIVE 타입
            inventoryEntities.add(InventoryEntity.builder()
                .productId(9991L)
                .quantity(100)
                .changeType(InventoryChangeType.RECEIVE)
                .build());

            // SALE 타입
            inventoryEntities.add(InventoryEntity.builder()
                .productId(9992L)
                .quantity(-50)
                .changeType(InventoryChangeType.SALE)
                .build());

            // ADJUST 타입
            inventoryEntities.add(InventoryEntity.builder()
                .productId(9993L)
                .quantity(30)
                .changeType(InventoryChangeType.ADJUST)
                .build());

            try {
                // 벌크 인서트 실행
                int result = jdbcInventoryRepository.bulkInsert(inventoryEntities);
                System.out.println("✅ Bulk insert successful! Inserted " + result + " records");

                // 저장된 데이터 확인
                List<Map<String, Object>> savedData = jdbcTemplate.queryForList(
                    "SELECT product_id, quantity, change_type FROM inventory WHERE product_id IN (9991, 9992, 9993) ORDER BY product_id"
                );

                System.out.println("\nVerifying saved data:");
                for (Map<String, Object> row : savedData) {
                    System.out.println(String.format("  Product ID: %s, Quantity: %s, Change Type: %s",
                        row.get("product_id"), row.get("quantity"), row.get("change_type")));
                }

                // 검증
                boolean allCorrect = true;
                if (!savedData.get(0).get("change_type").equals("RECEIVE")) {
                    System.out.println("❌ Error: First record change_type should be RECEIVE");
                    allCorrect = false;
                }
                if (!savedData.get(1).get("change_type").equals("SALE")) {
                    System.out.println("❌ Error: Second record change_type should be SALE");
                    allCorrect = false;
                }
                if (!savedData.get(2).get("change_type").equals("ADJUST")) {
                    System.out.println("❌ Error: Third record change_type should be ADJUST");
                    allCorrect = false;
                }

                if (allCorrect) {
                    System.out.println("\n✅ All change_type values are correct!");
                }

                // Cleanup
                jdbcTemplate.execute("DELETE FROM inventory WHERE product_id IN (9991, 9992, 9993)");

            } catch (Exception e) {
                System.err.println("❌ Error during bulk insert: " + e.getMessage());
                e.printStackTrace();
            }

            // Test 2: null change_type 처리 테스트
            System.out.println("\n\nTest 2: Testing null change_type handling");

            List<InventoryEntity> nullTypeEntities = new ArrayList<>();
            nullTypeEntities.add(InventoryEntity.builder()
                .productId(9994L)
                .quantity(200)
                .changeType(null)  // null로 설정
                .build());

            try {
                int result = jdbcInventoryRepository.bulkInsert(nullTypeEntities);
                System.out.println("✅ Bulk insert with null change_type successful! Inserted " + result + " records");

                Map<String, Object> savedData = jdbcTemplate.queryForMap(
                    "SELECT product_id, quantity, change_type FROM inventory WHERE product_id = 9994"
                );

                System.out.println(String.format("  Product ID: %s, Quantity: %s, Change Type: %s",
                    savedData.get("product_id"), savedData.get("quantity"), savedData.get("change_type")));

                if ("RECEIVE".equals(savedData.get("change_type"))) {
                    System.out.println("✅ Null change_type was correctly defaulted to RECEIVE");
                } else {
                    System.out.println("❌ Error: Null change_type should default to RECEIVE");
                }

                // Cleanup
                jdbcTemplate.execute("DELETE FROM inventory WHERE product_id = 9994");

            } catch (Exception e) {
                System.err.println("❌ Error during null change_type test: " + e.getMessage());
                e.printStackTrace();
            }

            // Test 3: 성능 테스트
            System.out.println("\n\nTest 3: Performance test with 100 records");

            List<InventoryEntity> perfTestEntities = new ArrayList<>();
            for (int i = 1; i <= 100; i++) {
                InventoryChangeType changeType = i % 3 == 0 ? InventoryChangeType.RECEIVE :
                                                i % 3 == 1 ? InventoryChangeType.SALE :
                                                InventoryChangeType.ADJUST;

                perfTestEntities.add(InventoryEntity.builder()
                    .productId(10000L + i)
                    .quantity(i * 10)
                    .changeType(changeType)
                    .build());
            }

            try {
                long startTime = System.currentTimeMillis();
                int result = jdbcInventoryRepository.bulkInsert(perfTestEntities);
                long endTime = System.currentTimeMillis();

                System.out.println("✅ Performance test: Inserted " + result + " records in " + (endTime - startTime) + "ms");

                // 데이터 개수 확인
                Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM inventory WHERE product_id BETWEEN 10001 AND 10100",
                    Integer.class
                );
                System.out.println("  Verified count: " + count + " records");

                // Cleanup
                jdbcTemplate.execute("DELETE FROM inventory WHERE product_id BETWEEN 10001 AND 10100");

            } catch (Exception e) {
                System.err.println("❌ Error during performance test: " + e.getMessage());
                e.printStackTrace();
            }

            System.out.println("\n========== BULK INSERT TEST COMPLETE ==========\n");

            // 애플리케이션 종료
            System.exit(0);
        };
    }
}