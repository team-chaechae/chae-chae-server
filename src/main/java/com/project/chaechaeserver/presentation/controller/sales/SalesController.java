package com.project.chaechaeserver.presentation.controller.sales;

import com.project.chaechaeserver.application.global.constants.ResCode;
import com.project.chaechaeserver.application.global.dto.ResDTO;
import com.project.chaechaeserver.application.response.sales.ResSalesGetByIdDTO;
import com.project.chaechaeserver.application.response.sales.ResSalesSearchDTO;
import com.project.chaechaeserver.application.service.sales.SalesService;
import com.project.chaechaeserver.infrastructure.sales.docs.SalesControllerSwagger;
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


    @GetMapping
    public ResponseEntity<ResDTO<ResSalesSearchDTO>> searchSalesByCondition(@RequestParam(required = false) Boolean deleted,
                                                                            @RequestParam(required = false) String productName,
                                                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate exactDate,
                                                                            @RequestParam(required = false) List<String> sort,
                                                                            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
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
