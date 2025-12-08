package com.project.orderservice.presentation.controller;

import com.project.orderservice.application.global.constants.ResCode;
import com.project.orderservice.application.global.dto.ResDTO;
import com.project.orderservice.application.response.ResSalesCreateDTO;
import com.project.orderservice.application.response.ResSalesGetByIdDTO;
import com.project.orderservice.application.response.ResSalesSearchDTO;
import com.project.orderservice.application.service.SalesService;
import com.project.orderservice.presentation.controller.docs.SalesControllerSwagger;
import com.project.orderservice.presentation.request.ReqCreateSalesDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sales")
public class SalesController implements SalesControllerSwagger {

    private final SalesService salesService;

    @Override
    @PostMapping
    public ResponseEntity<ResDTO<ResSalesCreateDTO>> createSales(@Valid @RequestBody ReqCreateSalesDTO dto) {
        return new ResponseEntity<>(
                ResDTO.<ResSalesCreateDTO>builder()
                        .code(ResCode.CREATED)
                        .message("판매 생성 완료")
                        .data(salesService.createSales(dto))
                        .build(),
                HttpStatus.CREATED
        );
    }

    @Override
    @GetMapping("/{salesId}")
    public ResponseEntity<ResDTO<ResSalesGetByIdDTO>> getSalesBySalesId(@PathVariable Long salesId) {
        return new ResponseEntity<>(
                ResDTO.<ResSalesGetByIdDTO>builder()
                        .code(ResCode.OK)
                        .message("판매기록 상세조회에 성공하였습니다")
                        .data(salesService.getSalesBySalesId(salesId))
                        .build(),
                HttpStatus.OK
        );
    }

    @Override
    @GetMapping
    public ResponseEntity<ResDTO<ResSalesSearchDTO>> searchSalesByCondition(
            @RequestParam(required = false) Boolean deleted,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate exactDate,
            @RequestParam(required = false) List<String> sort,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return new ResponseEntity<>(
                ResDTO.<ResSalesSearchDTO>builder()
                        .code(ResCode.OK)
                        .message("판매 기록 검색에 성공하였습니다.")
                        .data(salesService.searchSalesByCondition(
                                pageable, deleted, productName, startDate, endDate, exactDate, sort
                        ))
                        .build(),
                HttpStatus.OK
        );
    }
}
