package com.project.orderservice.application.service;

import com.project.orderservice.application.response.ResCreateOrderPostDTO;
import com.project.orderservice.application.response.ResOrdersSearchDTO;
import com.project.orderservice.application.response.ResUpdateOrderQuantityDTO;
import com.project.orderservice.application.response.ResUpdateOrderStatusDTO;
import com.project.orderservice.domain.model.constraint.StatusType;
import com.project.orderservice.presentation.request.ReqCreateOrderDTO;
import com.project.orderservice.presentation.request.ReqUpdateQuantityOrderDTO;
import com.project.orderservice.presentation.request.ReqUpdateStatusOrderDTO;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    ResCreateOrderPostDTO createOrderInfo(ReqCreateOrderDTO dto);

    ResOrdersSearchDTO searchOrdersByFilter(Pageable pageable, Long orderId, String productName, String productCategory,
        StatusType status, String createdBy, LocalDate startDate, LocalDate endDate, List<String> sortList);

    ResUpdateOrderStatusDTO updateStatusOrder(ReqUpdateStatusOrderDTO dto, Long orderId);

    ResUpdateOrderQuantityDTO updateQuantityOrder(ReqUpdateQuantityOrderDTO dto, Long orderId);
}
