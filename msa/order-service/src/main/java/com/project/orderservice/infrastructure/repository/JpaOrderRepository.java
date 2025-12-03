package com.project.orderservice.infrastructure.repository;

import com.project.orderservice.domain.model.OrderEntity;
import com.project.orderservice.domain.model.constraint.StatusType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaOrderRepository extends JpaRepository<OrderEntity, Long> {

    boolean existsByProductInfo_ProductIdAndStatus(Long productId, StatusType status);
}
