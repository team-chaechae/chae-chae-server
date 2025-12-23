package com.project.orderservice.presentation.controller.docs;

import com.project.orderservice.application.global.dto.ResDTO;
import com.project.orderservice.application.response.ResCreateOrderPostDTO;
import com.project.orderservice.application.response.ResOrdersSearchDTO;
import com.project.orderservice.application.response.ResUpdateOrderQuantityDTO;
import com.project.orderservice.application.response.ResUpdateOrderStatusDTO;
import com.project.orderservice.presentation.request.ReqCreateOrderDTO;
import com.project.orderservice.presentation.request.ReqUpdateQuantityOrderDTO;
import com.project.orderservice.presentation.request.ReqUpdateStatusOrderDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    @Operation(summary = "발주 조회", description = "발주 목록을 검색 조회하는 API입니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "검색 성공", content = @Content(schema = @Schema(implementation = ResOrdersSearchDTO.class))),
            @ApiResponse(responseCode = "400", description = "검색 실패.", content = @Content(schema = @Schema(implementation = ResDTO.class)))
    })
    @GetMapping
    ResponseEntity<ResDTO<ResOrdersSearchDTO>> searchOrdersByFilter(@RequestParam(required = false) Long orderId,
                                                                    @RequestParam(required = false) String productName,
                                                                    @RequestParam(required = false) String productCategory,
                                                                    @RequestParam(required = false) String status,
                                                                    @RequestParam(required = false) String createdBy,
                                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                                    @RequestParam(required = false) List<String> sort,
                                                                    @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable);

    @Operation(summary = "발주 상태 수정", description = "발주 상태를 수정하는 API입니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상태 수정 성공", content = @Content(schema = @Schema(implementation = ResUpdateOrderStatusDTO.class))),
            @ApiResponse(responseCode = "400", description = "상태 수정 실패.", content = @Content(schema = @Schema(implementation = ResDTO.class)))
    })
    @PatchMapping("/{orderId}/status")
    ResponseEntity<ResDTO<ResUpdateOrderStatusDTO>> updateStatusOrder(@Valid @RequestBody ReqUpdateStatusOrderDTO dto,
                                                                      @PathVariable Long orderId);


    @Operation(summary = "발주 수량 수정", description = "발주 수량을 수정하는 API입니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수량 수정 성공", content = @Content(schema = @Schema(implementation = ResUpdateOrderQuantityDTO.class))),
            @ApiResponse(responseCode = "400", description = "수량 수정 실패.", content = @Content(schema = @Schema(implementation = ResDTO.class)))
    })
    @PatchMapping("/{orderId}")
    ResponseEntity<ResDTO<ResUpdateOrderQuantityDTO>> updateQuantityOrder(@Valid @RequestBody ReqUpdateQuantityOrderDTO dto,
                                                                          @PathVariable Long orderId);
}
