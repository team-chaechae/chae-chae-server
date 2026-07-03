package com.project.chaechaeserver.domain.repository.order;

import com.project.chaechaeserver.domain.model.order.OrderCustomerEntity;
import com.project.chaechaeserver.domain.model.order.constraint.StatusType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface OrderCustomerRepository extends JpaRepository<OrderCustomerEntity, Long> {

    @Query("SELECT o FROM OrderCustomerEntity o WHERE o.status = :status AND o.expiresAt < :now")
    List<OrderCustomerEntity> findExpiredOrders(@Param("status") StatusType status, @Param("now") LocalDateTime now);

}
