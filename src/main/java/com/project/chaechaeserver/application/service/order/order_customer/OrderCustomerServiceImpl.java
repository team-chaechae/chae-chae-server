package com.project.chaechaeserver.application.service.order.order_customer;

import com.project.chaechaeserver.application.global.excepion.BadRequestException;
import com.project.chaechaeserver.application.global.excepion.EntityNotFoundException;
import com.project.chaechaeserver.application.response.order.ResCreateOrderCustomerPostDTO;
import com.project.chaechaeserver.application.service.inventory.InventoryCommonService;
import com.project.chaechaeserver.domain.model.inventory.constraint.InventoryChangeType;
import com.project.chaechaeserver.domain.model.order.OrderCustomerEntity;
import com.project.chaechaeserver.domain.model.order.constraint.StatusType;
import com.project.chaechaeserver.domain.repository.order.OrderCustomerRepository;
import com.project.chaechaeserver.presentation.request.order.ReqOrderCustomerPostCreateDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderCustomerServiceImpl implements OrderCustomerService {

    private final OrderCustomerRepository orderCustomerRepository;
    private final InventoryCommonService inventoryCommonService;

    @Override
    @Transactional
    public ResCreateOrderCustomerPostDTO createOrder(ReqOrderCustomerPostCreateDTO dto) {
        // 먼저 주문 엔티티 생성 및 저장
        OrderCustomerEntity orderCustomer = OrderCustomerEntity.createOrder(dto);
        OrderCustomerEntity savedOrder = orderCustomerRepository.save(orderCustomer);

        // 재고 차감 처리 (한 번의 순회로 처리)
        List<Long> productIds = new ArrayList<>();
        List<Integer> quantities = new ArrayList<>();

        savedOrder.getOrderItems().forEach(item -> {
            productIds.add(Long.valueOf(item.getProductId()));
            quantities.add(item.getQuantity());
        });

        inventoryCommonService.decreaseInventory(
            productIds,
            quantities,
            InventoryChangeType.SALE
        );

        return ResCreateOrderCustomerPostDTO.from(savedOrder, savedOrder.getOrderItems());
    }

    @Override
    @Transactional
    public void completeOrder(Long orderId) {
        OrderCustomerEntity order = orderCustomerRepository.findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다. orderId: " + orderId));

        if (!order.getStatus().canTransitionTo(StatusType.COMPLETED)) {
            throw new BadRequestException(
                String.format("주문 상태를 변경할 수 없습니다. 현재 상태: %s, 요청 상태: %s",
                    order.getStatus().getDisplayName(),
                    StatusType.COMPLETED.getDisplayName())
            );
        }

        order.updateStatus(StatusType.COMPLETED);
    }

    @Override
    public ResCreateOrderCustomerPostDTO getOrder(Long orderId) {
        OrderCustomerEntity order = orderCustomerRepository.findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다. orderId: " + orderId));

        return ResCreateOrderCustomerPostDTO.from(order, order.getOrderItems());
    }
}
