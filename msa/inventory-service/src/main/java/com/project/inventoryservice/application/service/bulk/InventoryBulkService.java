package com.project.inventoryservice.application.service.bulk;

import com.project.inventoryservice.application.response.bulk.ResBulkCreateInventoryPostDTO;
import com.project.inventoryservice.application.response.bulk.ResBulkSaleInventoryDTO;
import com.project.inventoryservice.application.response.bulk.ResUpdateInventoryDTO;
import com.project.inventoryservice.presentation.request.bulk.ReqBulkCreateInventoryDTO;
import com.project.inventoryservice.presentation.request.bulk.ReqUpdateInventoryDTO;

/**
 * 물류/재고 관리 전용 서비스
 *
 * 담당 영역:
 * - 대량 입고 처리
 * - 물류 출고 처리
 * - 재고 조정 (단건/대량)
 */
public interface InventoryBulkService {

    /**
     * 대량 입고 처리
     * @param dto 입고 요청 DTO
     * @return 입고 결과
     */
    ResBulkCreateInventoryPostDTO createInventory(ReqBulkCreateInventoryDTO dto);

    /**
     * 재고 조정 (단건/대량 통합)
     * @param dto 재고 조정 요청 DTO (단건 또는 여러건)
     * @return 조정 결과
     */
    ResUpdateInventoryDTO modifyInventory(ReqUpdateInventoryDTO dto);

    /**
     * 물류 출고 처리 (대량)
     * @param dto 출고 요청 DTO
     * @return 출고 결과
     */
    ResBulkSaleInventoryDTO decreaseInventoryForWarehouseShipment(ReqUpdateInventoryDTO dto);
}
