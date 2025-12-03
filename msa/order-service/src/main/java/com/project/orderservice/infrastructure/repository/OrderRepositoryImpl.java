package com.project.orderservice.infrastructure.repository;

import com.project.orderservice.domain.model.OrderEntity;
import com.project.orderservice.domain.model.constraint.StatusType;
import com.project.orderservice.domain.repository.OrderRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final JpaOrderRepository jpaOrderRepository;
    private final OrderQueryRepository orderQueryRepository;

    @Override
    public OrderEntity save(OrderEntity order) {
        return jpaOrderRepository.save(order);
    }

    @Override
    public Optional<OrderEntity> findById(Long orderId) {
        return jpaOrderRepository.findById(orderId);
    }

    @Override
    public boolean existsByProductInfo_ProductIdAndStatus(Long productId, StatusType status) {
        return jpaOrderRepository.existsByProductInfo_ProductIdAndStatus(productId, status);
    }

    @Override
    public Page<OrderEntity> searchOrdersByFilter(Pageable pageable, Long orderId, String productName,
        String productCategory, StatusType status, String createdBy, LocalDate startDate, LocalDate endDate,
        List<String> sortList) {
        return orderQueryRepository.searchOrdersByFilter(pageable, orderId, productName, productCategory, status,
            createdBy, startDate, endDate, sortList);
    }
}
