package com.project.orderservice.domain.repository;

import com.project.orderservice.domain.model.OrderEntity;
import com.project.orderservice.domain.model.constraint.StatusType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderRepository {

    OrderEntity save(OrderEntity order);

    Optional<OrderEntity> findById(Long orderId);

    boolean existsByProductInfo_ProductIdAndStatus(Long productId, StatusType status);

    Page<OrderEntity> searchOrdersByFilter(Pageable pageable, Long orderId, String productName, String productCategory,
        StatusType status, String createdBy, LocalDate startDate, LocalDate endDate, List<String> sortList);
}
