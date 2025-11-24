package com.project.chaechaeserver.application.service.inventory.bulk;

import com.project.chaechaeserver.application.response.inventory.bulk.ResBulkCreateInventoryPostDTO;
import com.project.chaechaeserver.application.response.inventory.bulk.ResBulkSaleInventoryDTO;
import com.project.chaechaeserver.application.response.inventory.bulk.ResUpdateInventoryDTO;
import com.project.chaechaeserver.presentation.request.inventory.bulk.ReqBulkCreateInventoryDTO;
import com.project.chaechaeserver.presentation.request.inventory.bulk.ReqUpdateInventoryDTO;

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