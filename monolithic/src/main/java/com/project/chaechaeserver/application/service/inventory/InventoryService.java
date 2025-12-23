package com.project.chaechaeserver.application.service.inventory;

import com.project.chaechaeserver.application.response.inventory.ResGetInventoryDTO;
import com.project.chaechaeserver.application.response.inventory.ResInventorySearchDTO;
import com.project.chaechaeserver.domain.model.products.constraint.ProductStatusType;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;

/**
 * 주문/판매 관련 재고 서비스
 *
 * 담당 영역:
 * - 주문에 의한 재고 감소
 * - 환불에 의한 재고 증가
 * - 재고 조회
 */
public interface InventoryService {


    /**
     * 재고 상세 조회
     * @param id 재고 ID
     * @return 재고 상세 정보
     */
    ResGetInventoryDTO getInventoryInfo(Long id);

    /**
     * 재고 검색
     * @return 검색 결과
     */
    ResInventorySearchDTO getInventorySearchInfo(Pageable pageable,
        String productName,
        Boolean deletedAt,
        ProductStatusType productStatus,
        Long productId,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate exactDate,
        List<String> sortList);
}
