package com.project.chaechaeserver.application.service.order.order_customer;

import com.project.chaechaeserver.application.response.order.ResCreateOrderCustomerPostDTO;
import com.project.chaechaeserver.presentation.request.order.ReqOrderCustomerPostCreateDTO;

/**
 * Redis 없이 DB를 직접 사용하는 주문 서비스 (성능 비교용)
 */
public interface OrderCustomerDbDirectService {

    /**
     * DB Pessimistic Lock을 사용한 주문 생성
     */
    ResCreateOrderCustomerPostDTO createOrderWithDbDirect(ReqOrderCustomerPostCreateDTO request);
}
