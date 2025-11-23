package com.project.chaechaeserver.application.service.inventory;

import com.project.chaechaeserver.application.response.inventory.InventoryWithProductDto;
import com.project.chaechaeserver.application.response.inventory.ResGetInventoryDTO;
import com.project.chaechaeserver.application.response.inventory.ResInventorySearchDTO;
import com.project.chaechaeserver.domain.model.products.constraint.ProductStatusType;
import com.project.chaechaeserver.domain.repository.inventory.InventoryRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문/판매 관련 재고 서비스 구현
 *
 * 책임:
 * - 주문 재고 감소 (도메인 서비스에 위임)
 * - 환불 재고 증가 (TODO)
 * - 재고 조회
 */
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

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
        Boolean deletedAt, ProductStatusType productStatus, Long productId, LocalDate startDate,
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
