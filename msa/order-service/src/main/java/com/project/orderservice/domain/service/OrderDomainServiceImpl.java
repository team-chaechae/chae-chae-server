package com.project.orderservice.domain.service;

import com.project.orderservice.application.global.exception.BadRequestException;
import com.project.orderservice.application.global.exception.EntityAlreadyExistException;
import com.project.orderservice.application.global.exception.EntityNotFoundException;
import com.project.orderservice.domain.model.OrderEntity;
import com.project.orderservice.domain.model.constraint.StatusType;
import com.project.orderservice.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderDomainServiceImpl implements OrderDomainService {

    private final OrderRepository orderRepository;

    @Override
    public void validateDuplicateOrder(Long productId) {
        if (orderRepository.existsByProductInfo_ProductIdAndStatus(productId, StatusType.APPROVED)) {
            throw new EntityAlreadyExistException("이미 발주 중인 상품입니다.");
        }
    }

    @Override
    public OrderEntity findById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("존재하지 않는 발주입니다."));
    }

    @Override
    public void validateStatusChange(OrderEntity order, StatusType targetStatus) {
        StatusType currentStatus = order.getStatus();

        if (currentStatus != StatusType.APPROVED) {
            throw new BadRequestException("승인 상태에서만 상태를 변경할 수 있습니다.");
        }

        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new BadRequestException(String.format(
                    "상태 '%s'(은)는 '%s'(으)로 변경할 수 없습니다.",
                    currentStatus.getDisplayName(),
                    targetStatus.getDisplayName()
            ));
        }
    }

    @Override
    public void validateQuantityUpdate(OrderEntity order) {
        if (order.getStatus() != StatusType.APPROVED) {
            throw new BadRequestException("승인 상태에서만 수량을 변경할 수 있습니다.");
        }
    }
}
