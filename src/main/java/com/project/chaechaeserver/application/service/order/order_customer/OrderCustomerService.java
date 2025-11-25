package com.project.chaechaeserver.application.service.order.order_customer;

import com.project.chaechaeserver.application.response.order.ResCreateOrderCustomerPostDTO;
import com.project.chaechaeserver.presentation.request.order.ReqOrderCustomerPostCreateDTO;

public interface OrderCustomerService {

    /**
     * 고객 주문 생성
     *
     * @param dto 주문 생성 요청 DTO
     * @return 생성된 주문 정보
     */
    ResCreateOrderCustomerPostDTO createOrder(ReqOrderCustomerPostCreateDTO dto);

    /**
     * 주문 완료 처리
     *
     * @param orderId 주문 ID
     */
    void completeOrder(Long orderId);

    /**
     * 주문 조회
     *
     * @param orderId 주문 ID
     * @return 주문 정보
     */
    ResCreateOrderCustomerPostDTO getOrder(Long orderId);
}
