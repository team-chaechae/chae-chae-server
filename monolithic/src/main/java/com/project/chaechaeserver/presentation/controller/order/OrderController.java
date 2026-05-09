package com.project.chaechaeserver.presentation.controller.order;

import static com.project.chaechaeserver.domain.model.user.constraint.RoleType.Role.ADMIN;

import com.project.chaechaeserver.application.global.constants.ResCode;
import com.project.chaechaeserver.application.global.dto.ResDTO;
import com.project.chaechaeserver.application.response.order.ResCreateOrderPostDTO;
import com.project.chaechaeserver.application.response.order.ResOrdersSearchDTO;
import com.project.chaechaeserver.application.response.order.ResUpdateOrderQuantityDTO;
import com.project.chaechaeserver.application.response.order.ResUpdateOrderStatusDTO;
import com.project.chaechaeserver.application.service.order.OrderService;
import com.project.chaechaeserver.domain.model.order.constraint.StatusType;
import com.project.chaechaeserver.infrastructure.orders.docs.OrdersControllerSwagger;
import com.project.chaechaeserver.presentation.request.order.ReqCreateOrderDTO;
import com.project.chaechaeserver.presentation.request.order.ReqUpdateQuantityOrderDTO;
import com.project.chaechaeserver.presentation.request.order.ReqUpdateStatusOrderDTO;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController implements OrdersControllerSwagger {

    private final OrderService orderService;

    @PostMapping
    @Secured(ADMIN)
    public ResponseEntity<ResDTO<ResCreateOrderPostDTO>> createOrder(@Valid @RequestBody ReqCreateOrderDTO dto) {

        return new ResponseEntity<>(
                ResDTO.<ResCreateOrderPostDTO>builder()
                        .code(ResCode.CREATED)
                        .message("발주 생성 완료")
                        .data(orderService.createOrderInfo(dto))
                        .build(),
                HttpStatus.CREATED
        );
    }

    @GetMapping
    @Secured(ADMIN)
    public ResponseEntity<ResDTO<ResOrdersSearchDTO>> searchOrdersByFilter(
            @RequestParam(required = false) Long orderId,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String productCategory,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) List<String> sort,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        StatusType statusType = StatusType.from(status);

        return new ResponseEntity<>(
                ResDTO.<ResOrdersSearchDTO>builder()
                        .code(ResCode.OK)
                        .message("발주 기록 검색 성공")
                        .data(orderService.searchOrdersByFilter(
                                pageable, orderId, productName, productCategory, statusType, createdBy, startDate,
                                endDate, sort
                        ))
                        .build(),
                HttpStatus.OK
        );
    }

    @PatchMapping("/{orderId}/status")
    @Secured(ADMIN)
    public ResponseEntity<ResDTO<ResUpdateOrderStatusDTO>> updateStatusOrder(@Valid @RequestBody ReqUpdateStatusOrderDTO dto, @PathVariable Long orderId) {
        return new ResponseEntity<>(
                ResDTO.<ResUpdateOrderStatusDTO>builder()
                        .code(ResCode.OK)
                        .message("발주 상태 수정 성공")
                        .data(orderService.updateStatusOrder(dto, orderId))
                        .build(),
                HttpStatus.OK
        );
    }

    @PatchMapping("/{orderId}")
    public ResponseEntity<ResDTO<ResUpdateOrderQuantityDTO>> updateQuantityOrder(@Valid @RequestBody ReqUpdateQuantityOrderDTO dto, @PathVariable Long orderId) {
        return new ResponseEntity<>(
                ResDTO.<ResUpdateOrderQuantityDTO>builder()
                        .code(ResCode.OK)
                        .message("발주 수량 수정 성공")
                        .data(orderService.updateQuantityOrder(dto, orderId))
                        .build(),
                HttpStatus.OK
        );
    }
}
