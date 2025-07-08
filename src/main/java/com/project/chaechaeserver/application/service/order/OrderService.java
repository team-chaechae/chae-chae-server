package com.project.chaechaeserver.application.service.order;

import com.project.chaechaeserver.application.response.order.ResCreateOrderPostDTO;
import com.project.chaechaeserver.presentation.request.order.ReqCreateOrderDTO;

public interface OrderService {

  ResCreateOrderPostDTO createOrderInfo(ReqCreateOrderDTO dto);

}
