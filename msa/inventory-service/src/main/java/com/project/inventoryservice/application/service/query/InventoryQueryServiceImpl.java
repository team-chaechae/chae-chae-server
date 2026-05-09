package com.project.inventoryservice.application.service.query;

import com.project.inventoryservice.application.response.InventoryWithProductDto;
import com.project.inventoryservice.application.response.ResGetInventoryDTO;
import com.project.inventoryservice.application.response.ResInventorySearchDTO;
import com.project.inventoryservice.domain.repository.InventoryRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 재고 조회 서비스 구현
 *
 * 책임:
 * - 재고 상세 조회
 * - 재고 검색/목록 조회
 */
@Service
@RequiredArgsConstructor
public class InventoryQueryServiceImpl implements InventoryQueryService {

    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional(readOnly = true)
    public ResGetInventoryDTO getInventoryInfo(Long id) {
        // 조인 쿼리로 한 번에 Inventory와 Product 정보 조회
        InventoryWithProductDto inventoryWithProduct = inventoryRepository.findInventoryWithProductById(id);

        if (inventoryWithProduct == null) {
            throw new IllegalArgumentException("Inventory not found with id: " + id);
        }

        return ResGetInventoryDTO.fromDto(inventoryWithProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public ResInventorySearchDTO getInventorySearchInfo(Pageable pageable, String productName,
        Boolean deletedAt, String productStatus, Long productId, LocalDate startDate,
        LocalDate endDate, LocalDate exactDate, List<String> sortList) {
        // Repository 인터페이스를 통해 Product 정보를 포함한 결과 조회
        return ResInventorySearchDTO.fromDto(
            inventoryRepository.findInventoryWithProduct(
                pageable, productName, deletedAt, productStatus, productId,
                startDate, endDate, exactDate, sortList
            )
        );
    }
}
