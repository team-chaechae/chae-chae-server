package com.project.orderservice.application.service;

import com.project.orderservice.application.response.ResSalesCreateDTO;
import com.project.orderservice.application.response.ResSalesGetByIdDTO;
import com.project.orderservice.application.response.ResSalesSearchDTO;
import com.project.orderservice.presentation.request.ReqCreateSalesDTO;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface SalesService {

    ResSalesCreateDTO createSales(ReqCreateSalesDTO dto);

    ResSalesGetByIdDTO getSalesBySalesId(Long salesId);

    ResSalesSearchDTO searchSalesByCondition(
            Pageable pageable,
            Boolean deletedCond,
            String productName,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate exactDate,
            List<String> sortList
    );

    /**
     * 결제 완료 처리 - 주문 상태를 COMPLETED로 변경
     */
    void completeSales(Long salesId, String orderId);

    /**
     * 주문 취소 처리 - 환불로 인한 주문 취소
     */
    void cancelSales(Long salesId, String orderId, String reason);
}
