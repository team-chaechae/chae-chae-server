package com.project.inventoryservice.application.service.sales;

import com.project.inventoryservice.application.response.sales.ResSalesInventoryDTO;
import com.project.inventoryservice.presentation.request.sales.ReqSalesInventoryDTO;

/**
 * 판매 재고 서비스
 * 주문/판매에 따른 재고 증감 처리
 */
public interface SalesInventoryService {

    /**
     * 재고 증가 (주문 취소 시 재고 복구)
     */
    ResSalesInventoryDTO increaseInventory(ReqSalesInventoryDTO dto);

    /**
     * 재고 차감 (주문 승인 시 재고 감소)
     */
    ResSalesInventoryDTO decreaseInventory(ReqSalesInventoryDTO dto);
}
