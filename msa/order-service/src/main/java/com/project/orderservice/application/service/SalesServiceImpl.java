package com.project.orderservice.application.service;

import com.project.orderservice.application.global.exception.BadRequestException;
import com.project.orderservice.application.response.ResSalesCreateDTO;
import com.project.orderservice.application.response.ResSalesGetByIdDTO;
import com.project.orderservice.application.response.ResSalesSearchDTO;
import com.project.orderservice.domain.model.SalesEntity;
import com.project.orderservice.domain.model.SalesItemEntity;
import com.project.orderservice.domain.repository.SalesRepository;
import com.project.orderservice.infrastructure.client.ProductCacheClient;
import com.project.orderservice.infrastructure.client.dto.ProductDTO;
import com.project.orderservice.infrastructure.kafka.InventoryEventProducer;
import com.project.orderservice.infrastructure.kafka.dto.InventoryReserveEvent;
import com.project.orderservice.presentation.request.ReqCreateSalesDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesServiceImpl implements SalesService {

    private final SalesRepository salesRepository;
    private final ProductCacheClient productCacheClient;
    private final InventoryEventProducer inventoryEventProducer;

    @Override
    @Transactional
    public ResSalesCreateDTO createSales(ReqCreateSalesDTO dto) {
        List<SalesItemEntity> salesItems = createSalesItems(dto.getSalesItems());
        SalesEntity sales = SalesEntity.createWithItems(salesItems);
        SalesEntity savedSales = salesRepository.save(sales);

        publishInventoryReserveEvent(savedSales.getId(), dto.getSalesItems());

        log.info("[Saga 시작] salesId: {}, 판매 생성 (PENDING), 상품 {}건",
                savedSales.getId(), savedSales.getItems().size());

        return ResSalesCreateDTO.from(savedSales);
    }


    private List<SalesItemEntity> createSalesItems(List<ReqCreateSalesDTO.SalesItem> items) {
        List<Long> productIds = items.stream()
                .map(ReqCreateSalesDTO.SalesItem::getProductId)
                .toList();

        Map<Long, ProductDTO> productMap = productCacheClient.getProductsByIds(productIds);

        return items.stream()
                .map(item -> {
                    ProductDTO product = productMap.get(item.getProductId());
                    if (product == null) {
                        throw new BadRequestException("존재하지 않는 상품입니다. productId: " + item.getProductId());
                    }
                    return SalesItemEntity.create(
                            item.getProductId(),
                            product.getName(),
                            item.getQuantity(),
                            product.getPrice()
                    );
                })
                .toList();
    }

    private void publishInventoryReserveEvent(Long salesId, List<ReqCreateSalesDTO.SalesItem> items) {
        List<InventoryReserveEvent.Item> inventoryItems = items.stream()
                .map(item -> InventoryReserveEvent.Item.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .build())
                .toList();

        InventoryReserveEvent event = InventoryReserveEvent.builder()
                .salesId(salesId)
                .items(inventoryItems)
                .build();

        inventoryEventProducer.publishReserveEvent(event);
    }

    /**
     * Saga 완료 처리 - Kafka Consumer에서 호출
     */
    @Transactional
    public void completeSales(Long salesId) {
        SalesEntity sales = salesRepository.findSalesBySalesId(salesId);
        sales.complete();
        log.info("[Saga 완료] salesId: {} COMPLETED", salesId);
    }

    /**
     * Saga 실패 처리 - Kafka Consumer에서 호출
     */
    @Transactional
    public void cancelSales(Long salesId, String reason) {
        SalesEntity sales = salesRepository.findSalesBySalesId(salesId);
        sales.cancel(reason);
        log.info("[Saga 실패] salesId: {} CANCELLED, 사유: {}", salesId, reason);
    }

    @Override
    @Transactional(readOnly = true)
    public ResSalesGetByIdDTO getSalesBySalesId(Long salesId) {
        return ResSalesGetByIdDTO.from(
                salesRepository.findSalesBySalesId(salesId)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ResSalesSearchDTO searchSalesByCondition(
            Pageable pageable,
            Boolean deletedCond,
            String productName,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate exactDate,
            List<String> sortList
    ) {
        return ResSalesSearchDTO.from(
                salesRepository.findSalesByDeletedAtIsNullWithCondition(
                        pageable, deletedCond, productName, startDate, endDate, exactDate, sortList
                )
        );
    }
}
