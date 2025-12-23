package com.project.inventoryservice.application.service.query;

import com.project.inventoryservice.application.response.ResGetInventoryDTO;
import com.project.inventoryservice.application.response.ResInventorySearchDTO;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;

/**
 * 재고 조회 서비스
 *
 * 담당 영역:
 * - 재고 상세 조회
 * - 재고 검색/목록 조회
 */
public interface InventoryQueryService {

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
        String productStatus,
        Long productId,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate exactDate,
        List<String> sortList);
}
