package com.project.chaechaeserver.order;

import com.project.chaechaeserver.application.response.order.ResCreateOrderCustomerPostDTO;
import com.project.chaechaeserver.application.service.order.order_customer.OrderCustomerService;
import com.project.chaechaeserver.domain.model.inventory.InventoryEntity;
import com.project.chaechaeserver.domain.model.inventory.constraint.InventoryChangeType;
import com.project.chaechaeserver.domain.model.order.OrderCustomerEntity;
import com.project.chaechaeserver.domain.model.products.ProductEntity;
import com.project.chaechaeserver.domain.repository.inventory.InventoryRepository;
import com.project.chaechaeserver.domain.repository.order.OrderCustomerRepository;
import com.project.chaechaeserver.domain.repository.products.ProductsRepository;
import com.project.chaechaeserver.domain.service.redis.RedisInventoryService;
import com.project.chaechaeserver.presentation.request.order.ReqOrderCustomerPostCreateDTO;
import com.project.chaechaeserver.presentation.request.order.ReqOrderCustomerPostCreateDTO.Order.OrderItem;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
public class RedisOrderPerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(RedisOrderPerformanceTest.class);

    @Autowired
    private OrderCustomerService orderCustomerService;

    @Autowired
    private OrderCustomerRepository orderCustomerRepository;

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private RedisInventoryService redisInventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    private ProductEntity createTestProduct(String productName) {
        // 테스트 상품 생성
        ProductEntity product = ProductEntity.createProducts(productName, "테스트 카테고리", 10000);
        product = productsRepository.save(product);

        // 초기 재고 설정 (1000개) - InventoryEntity로 관리
        InventoryEntity initialInventory = InventoryEntity.builder()
            .productId(product.getId())
            .quantity(1000)
            .changeType(InventoryChangeType.RECEIVE)
            .build();
        inventoryRepository.save(initialInventory);

        // Redis에 재고 초기화
        redisInventoryService.setStock(product.getId(), 1000);

        log.info("테스트 상품 생성 완료 - productId: {}, 초기 재고: 1000", product.getId());
        return product;
    }

    @Test
    @DisplayName("Redis 사용 - 동시 주문 100건 성능 테스트")
    @Transactional
    public void testOrderWithRedis() throws InterruptedException {
        // 테스트용 상품 생성
        ProductEntity testProduct = createTestProduct("Redis 테스트 상품");

        int threadCount = 100;
        int orderQuantity = 1;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final int orderIndex = i;
            executorService.submit(() -> {
                try {
                    ReqOrderCustomerPostCreateDTO request = createOrderRequest(
                        testProduct.getId(), orderQuantity, orderIndex);

                    ResCreateOrderCustomerPostDTO response = orderCustomerService.createOrder(request);

                    successCount.incrementAndGet();
                    log.debug("주문 성공 - orderId: {}", response.getOrderCustomer().getOrderId());
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.debug("주문 실패: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Redis 재고 확인
        Integer redisStock = redisInventoryService.getStock(testProduct.getId());

        log.info("========== Redis 사용 테스트 결과 ==========");
        log.info("총 주문 시도: {}", threadCount);
        log.info("성공: {}, 실패: {}", successCount.get(), failCount.get());
        log.info("소요 시간: {}ms", duration);
        log.info("Redis 최종 재고: {}", redisStock);
        log.info("예상 재고: {}", 1000 - successCount.get() * orderQuantity);
        log.info("==========================================");
    }

    @Test
    @DisplayName("Redis 미사용 - DB 직접 처리 (비교용)")
    @Transactional
    public void testOrderWithoutRedis() throws InterruptedException {
        // 테스트용 상품 생성
        ProductEntity testProduct = createTestProduct("DB 직접 테스트 상품");

        int threadCount = 100;
        int orderQuantity = 1;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final int orderIndex = i;
            executorService.submit(() -> {
                try {
                    // TransactionTemplate을 사용하여 트랜잭션 관리
                    transactionTemplate.execute(status -> {
                        createOrderDirectlyWithDB(testProduct.getId(), orderQuantity, orderIndex);
                        return null;
                    });

                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.debug("주문 실패: {}", e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // DB 재고 확인
        Integer currentStock = inventoryRepository.getCurrentStock(testProduct.getId());

        log.info("========== Redis 미사용 테스트 결과 ==========");
        log.info("총 주문 시도: {}", threadCount);
        log.info("성공: {}, 실패: {}", successCount.get(), failCount.get());
        log.info("소요 시간: {}ms", duration);
        log.info("DB 최종 재고: {}", currentStock);
        log.info("예상 재고: {}", 1000 - successCount.get() * orderQuantity);
        log.info("==========================================");
    }

    private void createOrderDirectlyWithDB(Long productId, int quantity, int orderIndex) {
        // 상품 조회
        ProductEntity product = productsRepository.findByIdForUpdate(productId);

        // 재고 확인 (InventoryRepository를 통해 조회)
        Integer currentStock = inventoryRepository.getCurrentStock(productId);
        if (currentStock == null || currentStock < quantity) {
            throw new RuntimeException("재고 부족");
        }

        // 재고 차감 (InventoryEntity 생성)
        InventoryEntity decreaseInventory = InventoryEntity.builder()
            .productId(productId)
            .quantity(-quantity)  // 음수로 차감
            .changeType(InventoryChangeType.ORDER_DECREASE)
            .build();
        inventoryRepository.save(decreaseInventory);

        // 주문 생성
        OrderItem item = OrderItem.builder()
            .productId(productId.intValue())
            .productName(product.getName())
            .quantity(quantity)
            .price(product.getPrice())
            .build();

        List<OrderItem> orderItems = List.of(item);

        ReqOrderCustomerPostCreateDTO.Order order = ReqOrderCustomerPostCreateDTO.Order.builder()
            .customerId((long) orderIndex)
            .orderItems(orderItems)
            .build();

        ReqOrderCustomerPostCreateDTO request = ReqOrderCustomerPostCreateDTO.builder()
            .order(order)
            .build();

        OrderCustomerEntity orderEntity = OrderCustomerEntity.createOrder(request);

        orderCustomerRepository.save(orderEntity);
    }

    private ReqOrderCustomerPostCreateDTO createOrderRequest(Long productId, int quantity,
        int orderIndex) {
        OrderItem item = OrderItem.builder()
            .productId(productId.intValue())
            .productName("테스트 상품")
            .quantity(quantity)
            .price(10000)
            .build();

        List<OrderItem> orderItems = List.of(item);

        ReqOrderCustomerPostCreateDTO.Order order = ReqOrderCustomerPostCreateDTO.Order.builder()
            .customerId((long) orderIndex)
            .orderItems(orderItems)
            .build();

        return ReqOrderCustomerPostCreateDTO.builder()
            .order(order)
            .build();
    }
}
