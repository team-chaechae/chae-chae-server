package com.project.chaechaeserver.infrastructure.orders;

import com.project.chaechaeserver.domain.model.order.OrderEntity;
import com.project.chaechaeserver.domain.model.order.constraint.StatusType;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderRepositoryCustom {

  Page<OrderEntity> searchOrdersByFilter(Pageable pageable, Long orderId, String productName, String productCategory,
      StatusType status, String createdBy , LocalDate startDate, LocalDate endDate, List<String> sortList);

}