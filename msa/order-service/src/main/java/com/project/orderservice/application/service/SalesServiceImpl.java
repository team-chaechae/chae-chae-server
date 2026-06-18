package com.project.orderservice.application.service;

import com.project.orderservice.application.event.DeliveryCancelRequestedInternalEvent;
import com.project.orderservice.application.event.DeliveryCreateRequestedInternalEvent;
import com.project.orderservice.application.event.OrderCreatedInternalEvent;
import com.project.orderservice.application.global.exception.BadRequestException;
import com.project.orderservice.application.response.ResSalesCreateDTO;
import com.project.orderservice.application.response.ResSalesGetByIdDTO;
import com.project.orderservice.application.response.ResSalesSearchDTO;
import com.project.orderservice.domain.model.SalesDeliveryStatusEntity;
import com.project.orderservice.domain.model.DeliveryAddressSnapshot;
import com.project.orderservice.domain.model.SalesEntity;
import com.project.orderservice.domain.model.SalesItemEntity;
import com.project.orderservice.domain.repository.SalesDeliveryStatusRepository;
import com.project.orderservice.domain.repository.SalesRepository;
import com.project.orderservice.infrastructure.client.InventoryFeignClient;
import com.project.orderservice.infrastructure.client.ProductCacheClient;
import com.project.orderservice.infrastructure.client.dto.ProductDTO;
import com.project.orderservice.infrastructure.client.dto.StockReservationDTO;
import com.project.orderservice.presentation.request.ReqCreateSalesDTO;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesServiceImpl implements SalesService {

    private final SalesRepository salesRepository;
    private final ProductCacheClient productCacheClient;
    private final InventoryFeignClient inventoryFeignClient;
    private final ApplicationEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry;
    private final SalesDeliveryStatusRepository salesDeliveryStatusRepository;

    /**
     * 주문 생성 (트랜잭셔널 아웃박스 패턴)
     *
     * - 주문 저장 + 이벤트 발행 (같은 트랜잭션)
     *   - @TransactionalEventListener(BEFORE_COMMIT): Outbox 저장
     *   - @TransactionalEventListener(AFTER_COMMIT): Kafka 발행
     */
    @Override
    @Transactional
    public ResSalesCreateDTO createSales(ReqCreateSalesDTO dto) {
        long startTime = System.currentTimeMillis();
        String orderId = UUID.randomUUID().toString();

        List<SalesItemEntity> salesItems = createSalesItems(dto.getSalesItems());
        SalesEntity sales = SalesEntity.createWithItems(
                orderId,
                dto.getUserId(),
                toDeliveryAddressSnapshot(dto.getDeliveryAddress()),
                salesItems
        );
        SalesEntity savedSales = salesRepository.save(sales);

        reserveStock(orderId, savedSales);
        registerReservationRollbackRelease(orderId, savedSales);
        publishOrderCreatedEvent(orderId, savedSales);

        log.info("[TIMING] 전체: {}ms", System.currentTimeMillis() - startTime);
        log.info("[주문 생성 완료 - 비동기 처리 시작] orderId: {}, salesId: {}, 상품 {}건, 총액: {}",
                orderId, savedSales.getId(), savedSales.getItems().size(), savedSales.getTotalPrice());

        meterRegistry.counter("order.sales.created").increment();

        return ResSalesCreateDTO.from(savedSales);
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
        var salesPage = salesRepository.findSalesByDeletedAtIsNullWithCondition(
                pageable, deletedCond, productName, startDate, endDate, exactDate, sortList
        );
        List<Long> salesIds = salesPage.getContent().stream()
                .map(SalesEntity::getId)
                .toList();
        Map<Long, SalesDeliveryStatusEntity> deliveryStatusBySalesId = findDeliveryStatusBySalesId(salesIds);

        return ResSalesSearchDTO.from(
                salesPage,
                deliveryStatusBySalesId
        );
    }

    private Map<Long, SalesDeliveryStatusEntity> findDeliveryStatusBySalesId(List<Long> salesIds) {
        if (salesIds.isEmpty()) {
            return Map.of();
        }
        return salesDeliveryStatusRepository.findBySalesIdIn(salesIds)
                .stream()
                .collect(Collectors.toMap(SalesDeliveryStatusEntity::getSalesId, status -> status));
    }

    @Override
    @Transactional
    public void completeSales(Long salesId, String orderId) {
        SalesEntity sales = salesRepository.findSalesBySalesIdSimple(salesId);

        if (sales == null) {
            log.warn("[주문 조회 실패] salesId: {} - 주문 없음", salesId);
            return;
        }

        if (sales.isCompleted()) {
            log.info("[주문 상태 변경 스킵 - 이미 완료] orderId: {}, salesId: {}", orderId, salesId);
            return;
        }
        if (sales.isCancelled()) {
            log.warn("[주문 상태 변경 스킵 - 이미 취소됨] orderId: {}, salesId: {}", orderId, salesId);
            return;
        }

        sales.complete();
        publishDeliveryCreateRequestedEvent(sales);
        recordE2EDurationIfPossible(sales, orderId);
        log.info("[주문 상태 완료] orderId: {}, salesId: {}, status: {}",
                orderId, salesId, sales.getStatus());

        meterRegistry.counter("order.sales.completed").increment();
    }

    @Override
    @Transactional
    public void cancelSales(Long salesId, String orderId, String reason) {
        SalesEntity sales = salesRepository.findSalesBySalesIdSimple(salesId);

        if (sales == null) {
            log.warn("[주문 조회 실패] salesId: {} - 주문 없음", salesId);
            return;
        }

        if (sales.isCancelled()) {
            log.info("[주문 상태 변경 스킵 - 이미 취소됨] orderId: {}, salesId: {}", orderId, salesId);
            return;
        }
        boolean shouldCancelDelivery = sales.isCompleted();
        sales.cancel(reason);
        if (shouldCancelDelivery) {
            publishDeliveryCancelRequestedEvent(orderId, salesId, reason);
        }
        log.info("[주문 취소 완료] orderId: {}, salesId: {}, status: {}",
                orderId, salesId, sales.getStatus());

        meterRegistry.counter("order.sales.cancelled").increment();
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
                .collect(Collectors.toList());
    }

    private void reserveStock(String orderId, SalesEntity sales) {
        StockReservationDTO.ReserveResponse response = inventoryFeignClient.reserveStock(
                StockReservationDTO.ReserveRequest.builder()
                        .orderId(orderId)
                        .salesId(sales.getId())
                        .items(toReserveItems(sales))
                        .build()
        );

        if (response == null || !response.isSuccess()) {
            String reason = response == null ? "응답 없음" : response.getFailureReason();
            throw new BadRequestException("재고 예약 실패: " + reason);
        }
    }

    private void registerReservationRollbackRelease(String orderId, SalesEntity sales) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    releaseReservedStock(orderId, sales, "주문 트랜잭션 롤백");
                }
            }
        });
    }

    private void releaseReservedStock(String orderId, SalesEntity sales, String reason) {
        try {
            StockReservationDTO.ReleaseResponse response = inventoryFeignClient.releaseStock(
                    StockReservationDTO.ReleaseRequest.builder()
                            .orderId(orderId)
                            .salesId(sales.getId())
                            .reason(reason)
                            .items(toReleaseItems(sales))
                            .build()
            );
            if (response == null || !response.isSuccess()) {
                String failureReason = response == null ? "응답 없음" : response.getMessage();
                log.error("[재고 예약 해제 실패] orderId: {}, salesId: {}, reason: {}, response: {}",
                        orderId, sales.getId(), reason, failureReason);
            }
        } catch (RuntimeException releaseFailure) {
            log.error("[재고 예약 해제 실패] orderId: {}, salesId: {}, reason: {}, error: {}",
                    orderId, sales.getId(), reason, releaseFailure.getMessage(), releaseFailure);
        }
    }

    private List<StockReservationDTO.ReserveRequest.ReserveItem> toReserveItems(SalesEntity sales) {
        return sales.getItems().stream()
                .map(item -> StockReservationDTO.ReserveRequest.ReserveItem.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .build())
                .toList();
    }

    private List<StockReservationDTO.ReleaseRequest.ReleaseItem> toReleaseItems(SalesEntity sales) {
        return sales.getItems().stream()
                .map(item -> StockReservationDTO.ReleaseRequest.ReleaseItem.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .build())
                .toList();
    }

    private DeliveryAddressSnapshot toDeliveryAddressSnapshot(ReqCreateSalesDTO.DeliveryAddress deliveryAddress) {
        return DeliveryAddressSnapshot.create(
                deliveryAddress.getRecipientName(),
                deliveryAddress.getRecipientPhone(),
                deliveryAddress.getZipCode(),
                deliveryAddress.getAddress(),
                deliveryAddress.getAddressDetail(),
                deliveryAddress.getDeliveryMemo()
        );
    }

    private void publishOrderCreatedEvent(String orderId, SalesEntity sales) {
        List<OrderCreatedInternalEvent.OrderItem> items = sales.getItems().stream()
                .map(item -> OrderCreatedInternalEvent.OrderItem.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                .collect(Collectors.toList());

        OrderCreatedInternalEvent event = OrderCreatedInternalEvent.of(
                orderId,
                sales.getId(),
                items,
                sales.getTotalPrice()
        );

        eventPublisher.publishEvent(event);
    }

    private void publishDeliveryCreateRequestedEvent(SalesEntity sales) {
        eventPublisher.publishEvent(DeliveryCreateRequestedInternalEvent.from(sales));
    }

    private void publishDeliveryCancelRequestedEvent(String orderId, Long salesId, String reason) {
        eventPublisher.publishEvent(DeliveryCancelRequestedInternalEvent.of(orderId, salesId, reason));
    }

    private void recordE2EDurationIfPossible(SalesEntity sales, String orderId) {
        LocalDateTime createdAt = sales.getCreatedAt();
        if (createdAt == null) {
            log.warn("[E2E] createdAt 없음 - orderId: {}, salesId: {}", orderId, sales.getId());
            return;
        }
        Duration duration = Duration.between(createdAt, LocalDateTime.now());
        meterRegistry.timer("order.sales.e2e.duration").record(duration);
        log.info("[E2E] orderId: {}, salesId: {}, durationMs: {}", orderId, sales.getId(), duration.toMillis());
    }
}
