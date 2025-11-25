package com.project.chaechaeserver.application.service.order.order_customer;

import com.project.chaechaeserver.application.global.excepion.BadRequestException;
import com.project.chaechaeserver.application.response.order.ResCreateOrderCustomerPostDTO;
import com.project.chaechaeserver.application.service.inventory.InventoryService;
import com.project.chaechaeserver.domain.model.inventory.InventoryEntity;
import com.project.chaechaeserver.domain.model.inventory.InventoryHistoryEntity;
import com.project.chaechaeserver.domain.model.order.OrderCustomerEntity;
import com.project.chaechaeserver.domain.model.order.OrderItemEntity;
import com.project.chaechaeserver.domain.model.products.ProductEntity;
import com.project.chaechaeserver.domain.repository.inventory.InventoryHistoryRepository;
import com.project.chaechaeserver.domain.repository.inventory.InventoryRepository;
import com.project.chaechaeserver.domain.repository.order.OrderCustomerRepository;
import com.project.chaechaeserver.domain.repository.products.ProductsRepository;
import com.project.chaechaeserver.presentation.request.inventory.bulk.ReqUpdateInventoryDTO;
import com.project.chaechaeserver.presentation.request.order.ReqOrderCustomerPostCreateDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Redis 없이 DB Pessimistic Lock만 사용하는 주문 서비스 (성능 비교용)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCustomerDbDirectServiceImpl implements OrderCustomerDbDirectService {

    private final ProductsRepository productsRepository;
    private final OrderCustomerRepository orderCustomerRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;
    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional
    public ResCreateOrderCustomerPostDTO createOrderWithDbDirect(ReqOrderCustomerPostCreateDTO request) {
        log.info("[DB 직접] 주문 생성 시작 - 고객ID: {}",
            request.getOrder().getCustomerId());

        // 1. 주문할 상품 ID 목록 추출
        List<Long> productIds = request.getOrder().getOrderItems().stream()
            .map(item -> item.getProductId().longValue())
            .toList();

        // 2. 상품 존재 여부 확인
        List<ProductEntity> products = productsRepository.findAllById(productIds);

        if (products.size() != productIds.size()) {
            throw new BadRequestException("일부 상품을 찾을 수 없습니다");
        }

        Map<Long, ProductEntity> productMap = products.stream()
            .collect(Collectors.toMap(ProductEntity::getId, p -> p));

        // 3. 현재 재고 조회 (히스토리 기반)
        Map<Long, Integer> currentStockMap = inventoryRepository.getCurrentStockMap(productIds);

        // 4. 재고 확인 및 차감 히스토리 생성
        List<InventoryEntity> inventoryHistories = new ArrayList<>();

        for (var orderItem : request.getOrder().getOrderItems()) {
            Long productId = orderItem.getProductId().longValue();
            Integer quantity = orderItem.getQuantity();

            ProductEntity product = productMap.get(productId);
            Integer currentStock = currentStockMap.getOrDefault(productId, 0);

            // 재고 확인
            if (currentStock < quantity) {
                throw new BadRequestException(
                    String.format("상품 '%s'의 재고가 부족합니다. (요청: %d, 재고: %d)",
                        product.getName(), quantity, currentStock));
            }

            // 재고 차감 히스토리 생성
            InventoryEntity history = InventoryEntity.builder()
                .productId(productId)
                .quantity(-quantity)  // 음수로 저장
                .changeType(com.project.chaechaeserver.domain.model.inventory.constraint.InventoryChangeType.ORDER_DECREASE)
                .build();
            inventoryHistories.add(history);

            log.info("[DB 직접] 재고 차감 완료 - 상품ID: {}, {} -> {}",
                productId, currentStock, currentStock - quantity);
        }

        // 5. 재고 히스토리 저장
        inventoryRepository.saveAll(inventoryHistories);

        // 4. 주문 아이템 생성
        List<OrderItemEntity> orderItems = request.getOrder().getOrderItems().stream()
            .map(item -> {
                ProductEntity product = productMap.get(item.getProductId().longValue());
                return OrderItemEntity.builder()
                    .productId(item.getProductId())
                    .productName(product.getName())
                    .quantity(item.getQuantity())
                    .price(product.getPrice())
                    .build();
            })
            .toList();

        // 5. 총 수량 및 금액 계산
        int totalQuantity = orderItems.stream()
            .mapToInt(OrderItemEntity::getQuantity)
            .sum();

        int totalPrice = orderItems.stream()
            .mapToInt(item -> item.getPrice() * item.getQuantity())
            .sum();

        // 6. 주문 생성 (createOrder 사용하지 않고 직접 빌더 사용)
        java.time.LocalDateTime expiresAt = java.time.LocalDateTime.now().plusSeconds(300);

        OrderCustomerEntity orderEntity = OrderCustomerEntity.builder()
            .customerId(request.getOrder().getCustomerId())
            .status(com.project.chaechaeserver.domain.model.order.constraint.StatusType.PENDING)
            .totalQuantity(totalQuantity)
            .totalPrice(totalPrice)
            .expiresAt(expiresAt)
            .build();

        // 7. 주문 아이템 연결 (cascade로 함께 저장됨)
        orderEntity.addOrderItems(orderItems);

        // 8. 주문 저장
        OrderCustomerEntity savedOrder = orderCustomerRepository.save(orderEntity);

        log.info("[DB 직접] 주문 생성 완료 - 주문ID: {}", savedOrder.getId());

        return ResCreateOrderCustomerPostDTO.from(savedOrder, savedOrder.getOrderItems());
    }
}
