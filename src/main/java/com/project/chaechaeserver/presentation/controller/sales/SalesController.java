package com.project.chaechaeserver.presentation.controller.sales;

import com.project.chaechaeserver.application.global.dto.ResDTO;
import com.project.chaechaeserver.application.response.sales.ResSalesGetByIdDTO;
import com.project.chaechaeserver.application.service.sales.SalesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sales")
public class SalesController {

    private final SalesService salesService;

    @GetMapping("/{salesId}")
    public ResponseEntity<ResDTO<ResSalesGetByIdDTO>> getSalesBySalesId(@PathVariable Long salesId) {
        return new ResponseEntity<>(
                ResDTO.<ResSalesGetByIdDTO>builder()
                        .code(HttpStatus.OK.value())
                        .message("판매기록 상세조회에 성공하였습니다")
                        .data(salesService.getSalesBySalesId(salesId))
                        .build(),
                HttpStatus.OK
        );
    }
}
