package com.project.orderservice.application.service;

import com.project.orderservice.application.response.ResCreateOrderPostDTO;
import com.project.orderservice.application.response.ResOrdersSearchDTO;
import com.project.orderservice.application.response.ResUpdateOrderQuantityDTO;
import com.project.orderservice.application.response.ResUpdateOrderStatusDTO;
import com.project.orderservice.domain.model.OrderEntity;
import com.project.orderservice.domain.model.ProductInfo;
import com.project.orderservice.domain.model.constraint.StatusType;
import com.project.orderservice.domain.repository.OrderRepository;
import com.project.orderservice.domain.service.OrderDomainService;
import com.project.orderservice.infrastructure.client.InventoryClient;
import com.project.orderservice.infrastructure.client.ProductClient;
import com.project.orderservice.infrastructure.client.dto.InventoryChangeDTO;
import com.project.orderservice.infrastructure.client.dto.ProductDTO;
import lombok.extern.slf4j.Slf4j;
import com.project.orderservice.presentation.request.ReqCreateOrderDTO;
import com.project.orderservice.presentation.request.ReqUpdateQuantityOrderDTO;
import com.project.orderservice.presentation.request.ReqUpdateStatusOrderDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderDomainService orderDomainService;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;

    @Override
    @Transactional
    public ResCreateOrderPostDTO createOrderInfo(ReqCreateOrderDTO dto) {
        Long productId = dto.getOrder().getProductId();
        Integer quantity = dto.getOrder().getQuantity();

        orderDomainService.validateDuplicateOrder(productId);

        StatusType status = StatusType.DEFAULT;

        // product-service에서 상품 정보 조회
        ProductDTO product = productClient.getProductById(productId);

        ProductInfo productInfo = new ProductInfo(
                product.getProductId(),
                product.getName(),
                product.getCategory(),
                product.getPrice()
        );

        OrderEntity order = OrderEntity.createOrder(productInfo, quantity, status);

        OrderEntity savedOrder = orderRepository.save(order);

        return ResCreateOrderPostDTO.from(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public ResOrdersSearchDTO searchOrdersByFilter(Pageable pageable, Long orderId,
                                                   String productName, String productCategory, StatusType status,
                                                   String createdBy, LocalDate startDate, LocalDate endDate,
                                                   List<String> sortList) {
        return ResOrdersSearchDTO.from(
                orderRepository.searchOrdersByFilter(
                        pageable, orderId, productName, productCategory, status, createdBy, startDate, endDate, sortList
                )
        );
    }

    @Override
    @Transactional
    public ResUpdateOrderStatusDTO updateStatusOrder(ReqUpdateStatusOrderDTO dto, Long orderId) {

        OrderEntity order = orderDomainService.findById(orderId);
        StatusType currentStatus = order.getStatus();
        StatusType newStatus = dto.getStatus();

        if (currentStatus == newStatus) {
            return ResUpdateOrderStatusDTO.from(order);
        }

        orderDomainService.validateStatusChange(order, newStatus);

        // 재고 처리 로직
        Long productId = order.getProductInfo().getProductId();
        Integer quantity = order.getQuantity();

        // PENDING → APPROVED: 재고 차감
        if (currentStatus == StatusType.PENDING && newStatus == StatusType.APPROVED) {
            log.info("[주문 승인] 재고 차감 요청 - productId: {}, quantity: {}", productId, quantity);
            InventoryChangeDTO.Response response = inventoryClient.decreaseInventory(
                    InventoryChangeDTO.Request.of(productId, quantity)
            );
            log.info("[주문 승인] 재고 차감 완료 - success: {}", response.isSuccess());
        }

        // APPROVED → CANCELLED: 재고 복구
        if (currentStatus == StatusType.APPROVED && newStatus == StatusType.CANCELLED) {
            log.info("[주문 취소] 재고 복구 요청 - productId: {}, quantity: {}", productId, quantity);
            InventoryChangeDTO.Response response = inventoryClient.increaseInventory(
                    InventoryChangeDTO.Request.of(productId, quantity)
            );
            log.info("[주문 취소] 재고 복구 완료 - success: {}", response.isSuccess());
        }

        order.updateStatus(newStatus);

        return ResUpdateOrderStatusDTO.from(order);
    }

    @Override
    @Transactional
    public ResUpdateOrderQuantityDTO updateQuantityOrder(ReqUpdateQuantityOrderDTO dto, Long orderId) {
        OrderEntity order = orderDomainService.findById(orderId);
        orderDomainService.validateQuantityUpdate(order);

        if (Objects.equals(order.getQuantity(), dto.getQuantity())) {
            return ResUpdateOrderQuantityDTO.from(order);
        }

        order.updateQuantity(dto.getQuantity());

        return ResUpdateOrderQuantityDTO.from(order);
    }

}
