package com.project.chaechaeserver.domain.repository.order;

import com.project.chaechaeserver.domain.model.order.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {

}
