package com.project.chaechaeserver.application.service.inventory;


import com.project.chaechaeserver.application.response.inventory.ResCreateInventoryPostDTO;
import com.project.chaechaeserver.domain.model.inventory.InventoryEntity;
import com.project.chaechaeserver.domain.model.products.ProductEntity;
import com.project.chaechaeserver.domain.repository.inventory.InventoryRepository;
import com.project.chaechaeserver.domain.repository.products.ProductsRepository;
import com.project.chaechaeserver.presentation.request.inventory.ReqCreateInventoryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductsRepository productsRepository;

    @Transactional
    public ResCreateInventoryPostDTO createInventory(ReqCreateInventoryDTO request) {


        ProductEntity product = productsRepository.findById(request.getInventory().getProductId())
            .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        InventoryEntity savedInventory = inventoryRepository.save(
            InventoryEntity.createInventory(
                product,
                request.getInventory().getQuantity()
            )
        );

        return ResCreateInventoryPostDTO.from(savedInventory);
    }
}
