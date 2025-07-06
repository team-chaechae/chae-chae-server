package com.project.chaechaeserver.presentation.controller.order;

import static com.project.chaechaeserver.domain.model.user.constraint.RoleType.Role.ADMIN;

import com.project.chaechaeserver.application.global.dto.ResDTO;
import com.project.chaechaeserver.application.response.order.ResCreateOrderPostDTO;
import com.project.chaechaeserver.application.service.order.OrderService;
import com.project.chaechaeserver.presentation.request.order.ReqCreateOrderDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

  private final OrderService orderService;

  @PostMapping
  @Secured(ADMIN)
  public ResponseEntity<ResDTO<ResCreateOrderPostDTO>> createOrder(
      @Valid @RequestBody ReqCreateOrderDTO request) {

    return new ResponseEntity<>(
        ResDTO.<ResCreateOrderPostDTO>builder()
            .code(HttpStatus.CREATED.value())
            .message("발주 생성 완료")
            .data(orderService.createOrderInfo(request))
            .build(),
        HttpStatus.CREATED
    );
  }
}
