package com.project.chaechaeserver.domain.service.order;

import com.project.chaechaeserver.application.global.excepion.EntityAlreadyExistException;
import com.project.chaechaeserver.application.global.excepion.EntityNotFoundException;
import com.project.chaechaeserver.domain.model.order.OrderEntity;
import com.project.chaechaeserver.domain.model.order.constraint.StatusType;
import com.project.chaechaeserver.domain.repository.order.OrderRepository;
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
    public void validateAlreadyCompleted(OrderEntity order, StatusType targetStatus) {
        if (order.getStatus() == StatusType.COMPLETED && targetStatus == StatusType.COMPLETED) {
            throw new EntityAlreadyExistException("이미 발주 완료된 상품입니다.");
        }
    }

}
