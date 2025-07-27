package com.project.chaechaeserver.infrastructure.orders.docs;

import com.project.chaechaeserver.application.global.dto.ResDTO;
import com.project.chaechaeserver.application.response.order.ResCreateOrderPostDTO;
import com.project.chaechaeserver.presentation.request.order.ReqCreateOrderDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Orders", description = "발주 관련 API를 제공합니다.")
@RequestMapping("/api/orders")
public interface OrdersControllerSwagger {

    @Operation(summary = "발주 생성", description = "발주를 생성하는 API입니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "발주 생성 성공", content = @Content(schema = @Schema(implementation = ResCreateOrderPostDTO.class))),
            @ApiResponse(responseCode = "400", description = "발주 생성 실패", content = @Content(schema = @Schema(implementation = ResDTO.class)))
    })
    @PostMapping
    ResponseEntity<ResDTO<ResCreateOrderPostDTO>> createOrder(@Valid @RequestBody ReqCreateOrderDTO dto);
}
