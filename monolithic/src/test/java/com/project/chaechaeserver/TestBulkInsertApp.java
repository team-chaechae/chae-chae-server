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
@Profile("bulk-test")
public class TestBulkInsertApp {

    public static void main(String[] args) {
        System.setProperty("spring.profiles.active", "dev,bulk-test");
        SpringApplication.run(TestBulkInsertApp.class, args);
    }

    @Bean
    @Profile("bulk-test")
    public CommandLineRunner testBulkInsert(
            JdbcInventoryRepository jdbcInventoryRepository,
            JdbcTemplate jdbcTemplate) {
        return args -> {
            System.out.println("\n========== BULK INSERT TEST START ==========\n");

            // Clean up any existing test data
            jdbcTemplate.execute("DELETE FROM inventory WHERE product_id IN (9991, 9992, 9993, 9994)");

            // Test: Verify change_type field is correctly saved
            System.out.println("Testing bulk insert with change_type field...");
            System.out.println("-------------------------------------------------");

            List<InventoryEntity> testEntities = new ArrayList<>();

            // RECEIVE type
            testEntities.add(InventoryEntity.builder()
                .productId(9991L)
                .quantity(100)
                .changeType(InventoryChangeType.RECEIVE)
                .build());

            // SALE type
            testEntities.add(InventoryEntity.builder()
                .productId(9992L)
                .quantity(-50)
                .changeType(InventoryChangeType.SALE)
                .build());

            // ADJUST type
            testEntities.add(InventoryEntity.builder()
                .productId(9993L)
                .quantity(30)
                .changeType(InventoryChangeType.ADJUST)
                .build());

            // null type (should default to RECEIVE)
            testEntities.add(InventoryEntity.builder()
                .productId(9994L)
                .quantity(200)
                .changeType(null)
                .build());

            try {
                // Execute bulk insert
                System.out.println("Executing bulk insert...");
                int result = jdbcInventoryRepository.bulkInsert(testEntities);
                System.out.println("✅ Bulk insert successful! Inserted " + result + " records");

                // Verify saved data
                List<Map<String, Object>> savedData = jdbcTemplate.queryForList(
                    "SELECT product_id, quantity, change_type FROM inventory " +
                    "WHERE product_id IN (9991, 9992, 9993, 9994) ORDER BY product_id"
                );

                System.out.println("\n=== Verification Results ===");

                boolean allPassed = true;
                for (Map<String, Object> row : savedData) {
                    Long productId = ((Number) row.get("product_id")).longValue();
                    String changeType = (String) row.get("change_type");
                    Integer quantity = ((Number) row.get("quantity")).intValue();

                    String expected = "";
                    boolean passed = false;

                    if (productId == 9991L) {
                        expected = "RECEIVE";
                        passed = "RECEIVE".equals(changeType);
                    } else if (productId == 9992L) {
                        expected = "SALE";
                        passed = "SALE".equals(changeType);
                    } else if (productId == 9993L) {
                        expected = "ADJUST";
                        passed = "ADJUST".equals(changeType);
                    } else if (productId == 9994L) {
                        expected = "RECEIVE (default from null)";
                        passed = "RECEIVE".equals(changeType);
                    }

                    String status = passed ? "✅ PASS" : "❌ FAIL";
                    System.out.printf("%s Product %d: quantity=%d, change_type=%s (expected: %s)%n",
                        status, productId, quantity, changeType, expected);

                    if (!passed) allPassed = false;
                }

                System.out.println("\n=== Test Summary ===");
                if (allPassed) {
                    System.out.println("🎉 ALL TESTS PASSED! 🎉");
                    System.out.println("The bulk insert change_type field is working correctly!");
                    System.out.println("null values are correctly defaulting to RECEIVE");
                } else {
                    System.out.println("❌ Some tests failed. Check the output above.");
                }

                // Cleanup
                jdbcTemplate.execute("DELETE FROM inventory WHERE product_id IN (9991, 9992, 9993, 9994)");
                System.out.println("\nTest data cleaned up.");

            } catch (Exception e) {
                System.err.println("❌ Error during test: " + e.getMessage());
                e.printStackTrace();
            }

            System.out.println("\n========== BULK INSERT TEST COMPLETE ==========\n");

            // Exit application
            System.exit(0);
        };
    }
}