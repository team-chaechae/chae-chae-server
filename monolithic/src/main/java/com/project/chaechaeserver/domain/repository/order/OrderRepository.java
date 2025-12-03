package com.project.chaechaeserver.domain.repository.order;

import com.project.chaechaeserver.domain.model.order.OrderEntity;
import com.project.chaechaeserver.domain.model.order.constraint.StatusType;
import com.project.chaechaeserver.infrastructure.orders.OrderRepositoryCustom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, Long>, OrderRepositoryCustom {

  boolean existsByProductInfo_ProductIdAndStatus(Long productId, StatusType status);
}
