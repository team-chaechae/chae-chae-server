package com.project.orderservice.domain.service;

import com.project.orderservice.domain.model.OrderEntity;
import com.project.orderservice.domain.model.constraint.StatusType;

public interface OrderDomainService {

    void validateDuplicateOrder(Long productId);

    OrderEntity findById(Long orderId);

    void validateStatusChange(OrderEntity order, StatusType status);

    void validateQuantityUpdate(OrderEntity order);
}
