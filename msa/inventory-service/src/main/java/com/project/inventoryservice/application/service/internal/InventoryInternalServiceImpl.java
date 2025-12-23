package com.project.inventoryservice.application.service.internal;

import com.project.inventoryservice.application.response.internal.ResInventoryChangeDTO;
import com.project.inventoryservice.application.response.internal.ResInventoryChangeDTO.InventoryChangeResult;
import com.project.inventoryservice.domain.model.InventoryEntity;
import com.project.inventoryservice.domain.model.constraint.InventoryChangeType;
import com.project.inventoryservice.domain.repository.InventoryRepository;
import com.project.inventoryservice.presentation.request.internal.ReqInventoryChangeDTO;
import com.project.inventoryservice.presentation.request.internal.ReqInventoryChangeDTO.InventoryChangeItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryInternalServiceImpl implements InventoryInternalService {

    private final InventoryRepository inventoryRepository;

    @Override
    @Transactional
    public ResInventoryChangeDTO increaseInventory(ReqInventoryChangeDTO dto) {
        log.info("[Internal] 재고 증가 요청: {} 건", dto.getItems().size());

        List<InventoryEntity> inventories = new ArrayList<>();
        List<Long> productIds = new ArrayList<>();

        for (InventoryChangeItem item : dto.getItems()) {
            InventoryEntity inventory = InventoryEntity.builder()
                    .productId(item.getProductId())
                    .quantity(Math.abs(item.getQuantity()))
                    .changeType(InventoryChangeType.ORDER_RESTORE)
                    .build();
            inventories.add(inventory);
            productIds.add(item.getProductId());
        }

        inventoryRepository.saveAll(inventories);

        // 변경 후 현재 재고 조회
        Map<Long, Integer> currentStockMap = inventoryRepository.getCurrentStockMap(productIds);

        List<InventoryChangeResult> results = dto.getItems().stream()
                .map(item -> InventoryChangeResult.builder()
                        .productId(item.getProductId())
                        .changedQuantity(item.getQuantity())
                        .currentStock(currentStockMap.getOrDefault(item.getProductId(), 0))
                        .build())
                .collect(Collectors.toList());

        return ResInventoryChangeDTO.success(results);
    }

    @Override
    @Transactional
    public ResInventoryChangeDTO decreaseInventory(ReqInventoryChangeDTO dto) {
        log.info("[Internal] 재고 차감 요청: {} 건", dto.getItems().size());

        List<InventoryEntity> inventories = new ArrayList<>();
        List<Long> productIds = new ArrayList<>();

        for (InventoryChangeItem item : dto.getItems()) {
            InventoryEntity inventory = InventoryEntity.builder()
                    .productId(item.getProductId())
                    .quantity(-Math.abs(item.getQuantity()))
                    .changeType(InventoryChangeType.ORDER_DECREASE)
                    .build();
            inventories.add(inventory);
            productIds.add(item.getProductId());
        }

        inventoryRepository.saveAll(inventories);

        // 변경 후 현재 재고 조회
        Map<Long, Integer> currentStockMap = inventoryRepository.getCurrentStockMap(productIds);

        List<InventoryChangeResult> results = dto.getItems().stream()
                .map(item -> InventoryChangeResult.builder()
                        .productId(item.getProductId())
                        .changedQuantity(-Math.abs(item.getQuantity()))
                        .currentStock(currentStockMap.getOrDefault(item.getProductId(), 0))
                        .build())
                .collect(Collectors.toList());

        return ResInventoryChangeDTO.success(results);
    }
}
