package com.project.chaechaeserver.domain.service.order;

import com.project.chaechaeserver.domain.model.order.OrderEntity;
import com.project.chaechaeserver.domain.model.order.constraint.StatusType;

public interface OrderDomainService {

    void validateDuplicateOrder(Long productId);

    OrderEntity findById(Long orderId);

    void validateStatusChange(OrderEntity order, StatusType status);

    void validateQuantityUpdate(OrderEntity order);
}