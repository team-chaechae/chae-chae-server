package com.project.inventoryservice.application.service.internal;

import com.project.inventoryservice.application.response.internal.ResInventoryChangeDTO;
import com.project.inventoryservice.presentation.request.internal.ReqInventoryChangeDTO;

/**
 * 내부 서비스 호출용 재고 서비스
 * order-service 등 다른 MSA 서비스에서 호출
 */
public interface InventoryInternalService {

    /**
     * 재고 증가 (주문 취소 시 재고 복구)
     * @param dto 재고 증가 요청
     * @return 처리 결과
     */
    ResInventoryChangeDTO increaseInventory(ReqInventoryChangeDTO dto);

    /**
     * 재고 차감 (주문 승인 시 재고 감소)
     * @param dto 재고 차감 요청
     * @return 처리 결과
     */
    ResInventoryChangeDTO decreaseInventory(ReqInventoryChangeDTO dto);
}
