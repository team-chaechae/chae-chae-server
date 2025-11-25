package com.project.chaechaeserver.presentation.controller.order;

import com.project.chaechaeserver.application.global.dto.ResDTO;
import com.project.chaechaeserver.application.response.order.ResCreateOrderCustomerPostDTO;
import com.project.chaechaeserver.application.service.order.order_customer.OrderCustomerDbDirectService;
import com.project.chaechaeserver.application.service.order.order_customer.OrderCustomerService;
import com.project.chaechaeserver.presentation.controller.order.docs.OrderCustomerControllerSwagger;
import com.project.chaechaeserver.presentation.request.order.ReqOrderCustomerPostCreateDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/customer-orders")
public class OrderCustomerController implements OrderCustomerControllerSwagger {

    private final OrderCustomerService orderCustomerService;
    private final OrderCustomerDbDirectService orderCustomerDbDirectService;

    @Override

    @PostMapping
    public ResponseEntity<ResDTO<ResCreateOrderCustomerPostDTO>> createOrder(
        @Valid @RequestBody ReqOrderCustomerPostCreateDTO request) {

        log.info("고객 주문 생성 요청 - 고객ID: {}, 상품 수: {}",
            request.getOrder().getCustomerId(),
            request.getOrder().getOrderItems().size());

        ResCreateOrderCustomerPostDTO response = orderCustomerService.createOrder(request);

        return new ResponseEntity<>(
            ResDTO.<ResCreateOrderCustomerPostDTO>builder()
                .code(HttpStatus.CREATED.value())
                .message("주문이 성공적으로 생성되었습니다")
                .data(response)
                .build(),
            HttpStatus.CREATED
        );
    }

    @Override
    @GetMapping("/{orderId}")
    public ResponseEntity<ResDTO<ResCreateOrderCustomerPostDTO>> getOrder(
        @PathVariable Long orderId) {

        log.info("주문 조회 요청 - 주문ID: {}", orderId);

        ResCreateOrderCustomerPostDTO response = orderCustomerService.getOrder(orderId);

        return new ResponseEntity<>(
            ResDTO.<ResCreateOrderCustomerPostDTO>builder()
                .code(HttpStatus.OK.value())
                .message("주문 조회가 완료되었습니다")
                .data(response)
                .build(),
            HttpStatus.OK
        );
    }

    @Override
    @PatchMapping("/{orderId}/complete")
    public ResponseEntity<ResDTO<Void>> completeOrder(
        @PathVariable Long orderId) {

        log.info("주문 완료 요청 - 주문ID: {}", orderId);

        orderCustomerService.completeOrder(orderId);

        return new ResponseEntity<>(
            ResDTO.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("주문이 완료되었습니다")
                .build(),
            HttpStatus.OK
        );
    }

    /**
     * DB 직접 사용 주문 생성 (성능 비교용)
     * Redis 캐싱 없이 Pessimistic Lock으로 재고 차감
     */
    @PostMapping("/db-direct")
    public ResponseEntity<ResDTO<ResCreateOrderCustomerPostDTO>> createOrderWithDbDirect(
        @Valid @RequestBody ReqOrderCustomerPostCreateDTO request) {

        log.info("[DB 직접] 고객 주문 생성 요청 - 고객ID: {}, 상품 수: {}",
            request.getOrder().getCustomerId(),
            request.getOrder().getOrderItems().size());

        ResCreateOrderCustomerPostDTO response = orderCustomerDbDirectService.createOrderWithDbDirect(request);

        return new ResponseEntity<>(
            ResDTO.<ResCreateOrderCustomerPostDTO>builder()
                .code(HttpStatus.CREATED.value())
                .message("주문이 성공적으로 생성되었습니다 (DB 직접 방식)")
                .data(response)
                .build(),
            HttpStatus.CREATED
        );
    }
}
