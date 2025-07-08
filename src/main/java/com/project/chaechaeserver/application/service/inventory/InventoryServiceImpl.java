package com.project.chaechaeserver.application.service.inventory;


import com.project.chaechaeserver.application.response.inventory.ResCreateInventoryPostDTO;
import com.project.chaechaeserver.application.response.inventory.ResUpdateInventoryPostDTO;
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
    public ResCreateInventoryPostDTO addInventory(ReqCreateInventoryDTO dto) {

        ProductEntity product = productsRepository.findById(dto.getInventory().getProductId())
            .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        InventoryEntity savedInventory = product.addInventory(dto.getInventory().getQuantity());

        return ResCreateInventoryPostDTO.from(savedInventory);
    }

    @Override
    @Transactional
    public ResUpdateInventoryPostDTO decreaseInventory(ReqCreateInventoryDTO dto) {

        ProductEntity product = productsRepository.findById(dto.getInventory().getProductId())
            .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

        InventoryEntity savedInventory = product.decreaseInventory(dto.getInventory().getQuantity());

        return ResUpdateInventoryPostDTO.from(savedInventory);
    }

}
