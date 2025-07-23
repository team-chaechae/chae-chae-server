package com.project.chaechaeserver.application.service.order;

import com.project.chaechaeserver.application.response.order.ResCreateOrderPostDTO;
import com.project.chaechaeserver.application.response.order.ResOrdersSearchDTO;
import com.project.chaechaeserver.application.response.order.ResUpdateOrderDTO;
import com.project.chaechaeserver.domain.model.order.constraint.StatusType;
import com.project.chaechaeserver.presentation.request.order.ReqCreateOrderDTO;
import com.project.chaechaeserver.presentation.request.order.ReqUpdateOrderDTO;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    ResCreateOrderPostDTO createOrderInfo(ReqCreateOrderDTO dto);

    ResOrdersSearchDTO searchOrdersByFilter(Pageable pageable, Long orderId, String productName,
                                            String productCategory, StatusType status, String createdBy,
                                            LocalDate startDate, LocalDate endDate, List<String> sort);

    ResUpdateOrderDTO updateOrderStatus(ReqUpdateOrderDTO dto, Long orderId);

}
