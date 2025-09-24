package com.project.chaechaeserver.application.service.inventory;


import com.project.chaechaeserver.application.response.inventory.ResGetInventoryDTO;
import com.project.chaechaeserver.application.response.inventory.ResInventorySearchDTO;
import com.project.chaechaeserver.application.response.inventory.ResVulkCreateInventoryPostDTO;
import com.project.chaechaeserver.application.response.inventory.ResSingleUpdateInventoryDTO;
import com.project.chaechaeserver.application.response.inventory.ResVulkSaleInventoryDTO;
import com.project.chaechaeserver.application.response.inventory.ResVulkUpdateInventoryDTO;
import com.project.chaechaeserver.domain.model.inventory.InventoryEntity;
import com.project.chaechaeserver.domain.model.products.ProductEntity;
import com.project.chaechaeserver.domain.model.products.constraint.ProductStatusType;
import com.project.chaechaeserver.domain.repository.inventory.InventoryRepository;
import com.project.chaechaeserver.domain.repository.products.ProductsRepository;
import com.project.chaechaeserver.domain.service.products.ProductDomainService;
import com.project.chaechaeserver.presentation.request.inventory.ReqSingleUpdateInventoryDTO;
import com.project.chaechaeserver.presentation.request.inventory.ReqVulkCreateInventoryDTO;
import com.project.chaechaeserver.presentation.request.inventory.ReqVulkUpdateInventoryDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductsRepository productsRepository;
    private final ProductDomainService productDomainService;

    @Override
    @Transactional
    public ResVulkCreateInventoryPostDTO createInventoryForVulk(ReqVulkCreateInventoryDTO dto) {
        List<Long> ids = dto.getInventory()
            .stream().map(ReqVulkCreateInventoryDTO.Inventory::getProductId)
            .toList();

        List<Integer> quantities = dto.getInventory()
            .stream().map(ReqVulkCreateInventoryDTO.Inventory::getQuantity)
            .toList();

        List<ProductEntity> validatedProducts = productDomainService.validateProductIds(ids);

        Map<Long, ProductEntity> productEntityMap = validatedProducts.stream()
            .collect(Collectors.toMap(ProductEntity::getId, p -> p));

        List<InventoryEntity> inventoryEntities = ProductEntity.createInventoryEntities(ids,quantities, productEntityMap);

        ProductEntity.updateQuantitiesBulk(ids, quantities, productEntityMap);

        List<InventoryEntity> savedInventoryEntities = inventoryRepository.saveAll(inventoryEntities);

        return ResVulkCreateInventoryPostDTO.from(savedInventoryEntities);

    }
    @Override
    @Transactional
    public ResSingleUpdateInventoryDTO modifyInventoryForSingle(ReqSingleUpdateInventoryDTO dto) {

        ProductEntity product = productsRepository.findByIdForUpdate(
            dto.getInventory().getProductId());

        InventoryEntity newHistory = product.modifyInventory(dto.getInventory().getQuantity());
        InventoryEntity savedHistory = inventoryRepository.save(newHistory);


        return ResSingleUpdateInventoryDTO.from(product, savedHistory);

    }
    @Override
    @Transactional
    public ResVulkUpdateInventoryDTO modifyInventoryForVulk(ReqVulkUpdateInventoryDTO dto) {
        List<Long> ids = dto.getInventory()
            .stream().map(ReqVulkUpdateInventoryDTO.Inventory::getProductId)
            .toList();

        List<Integer> quantities = dto.getInventory()
            .stream().map(ReqVulkUpdateInventoryDTO.Inventory::getQuantity)
            .toList();

        List<ProductEntity> validatedProducts = productDomainService.validateProductIds(ids);
        Map<Long, ProductEntity> productEntityMap = validatedProducts.stream()
            .collect(Collectors.toMap(ProductEntity::getId, p -> p));

        List<InventoryEntity> createdHistories = ProductEntity.modifyQuantitiesBulk(ids, quantities, productEntityMap);

        return ResVulkUpdateInventoryDTO.from(createdHistories);
    }


    @Override
    @Transactional
    public ResVulkSaleInventoryDTO decreaseInventoryForBulk(ReqVulkUpdateInventoryDTO dto) {

        List<Long> productIds = dto.getInventory()
            .stream()
            .map(ReqVulkUpdateInventoryDTO.Inventory::getProductId)
            .toList();

        List<Integer> quantities = dto.getInventory()
            .stream()
            .map(ReqVulkUpdateInventoryDTO.Inventory::getQuantity)
            .toList();

        List<ProductEntity> validatedProducts = productDomainService.validateProductIds(productIds);
        Map<Long, ProductEntity> productEntityMap = validatedProducts.stream()
            .collect(Collectors.toMap(ProductEntity::getId, p -> p));

        ProductEntity.validateSufficientStock(productIds, quantities, productEntityMap);

        List<InventoryEntity> inventoryEntities = ProductEntity.createSaleInventoryEntities(productIds, quantities, productEntityMap);

        ProductEntity.decreaseQuantitiesBulk(productIds, quantities, productEntityMap);

        List<InventoryEntity> savedInventoryEntities = inventoryRepository.saveAll(inventoryEntities);

        return ResVulkSaleInventoryDTO.from(savedInventoryEntities);
    }


    @Override
    @Transactional(readOnly = true)
    public ResGetInventoryDTO getInventoryInfo(Long id) {

        InventoryEntity inventoryInfo = inventoryRepository.findInventoryByInventoryId(id);

        return ResGetInventoryDTO.from(inventoryInfo);
    }

    @Override
    @Transactional(readOnly = true)
    public ResInventorySearchDTO getInventorySearchInfo(Pageable pageable, String productName,
        Boolean deletedAt, ProductStatusType productStatus, Long productId, LocalDate startDate,
        LocalDate endDate, LocalDate exactDate, List<String> sortList) {
        return ResInventorySearchDTO.from(inventoryRepository.findInventoryByDeletedAtIsNullWithCondition(pageable,productName, deletedAt, productStatus, productId, startDate, endDate, exactDate, sortList));
    }


}