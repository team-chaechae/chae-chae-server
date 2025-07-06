package com.project.chaechaeserver.domain.repository.order;

import com.project.chaechaeserver.domain.model.order.OrderEntity;
import com.project.chaechaeserver.domain.model.order.constraint.StatusType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

  boolean existsByProductIdAndStatus(Long productId, StatusType statusType);
}
