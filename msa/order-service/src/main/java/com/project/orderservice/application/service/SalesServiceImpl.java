package com.project.orderservice.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.orderservice.application.global.exception.BadRequestException;
import com.project.orderservice.application.response.ResSalesCreateDTO;
import com.project.orderservice.application.response.ResSalesGetByIdDTO;
import com.project.orderservice.application.response.ResSalesSearchDTO;
import com.project.orderservice.domain.model.OutboxEntity;
import com.project.orderservice.domain.model.SalesEntity;
import com.project.orderservice.domain.model.SalesItemEntity;
import com.project.orderservice.domain.repository.OutboxRepository;
import com.project.orderservice.domain.repository.SalesRepository;
import com.project.orderservice.infrastructure.client.ProductCacheClient;
import com.project.orderservice.infrastructure.client.dto.ProductDTO;
import com.project.orderservice.infrastructure.kafka.OrderEventProducer;
import com.project.orderservice.infrastructure.kafka.dto.OrderCreatedEvent;
import com.project.orderservice.presentation.request.ReqCreateSalesDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesServiceImpl implements SalesService {

    private static final String TOPIC_ORDER_CREATED = "order-created";

    private final SalesRepository salesRepository;
    private final OutboxRepository outboxRepository;
    private final ProductCacheClient productCacheClient;
    private final OrderEventProducer orderEventProducer;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 주문 생성 (비동기 처리)
     *
     * 1. 상품 정보 조회 (Redis)
     * 2. 주문 저장 (DB) - 상태: PENDING
     * 3. Kafka 이벤트 발행 (order-created)
     * 4. HTTP 응답 반환 (빠름!)
     *
     * 이후 비동기로:
     * - inventory-service: 재고 예약
     * - payment-service: 결제 처리
     * - order-service: 주문 상태 업데이트 (payment-completed 이벤트 수신 시)
     */
    @Override
    public ResSalesCreateDTO createSales(ReqCreateSalesDTO dto) {
        long startTime = System.currentTimeMillis();
        String orderId = UUID.randomUUID().toString();

        // 1. 주문 아이템 생성 및 상품 정보 조회 (Redis)
        long t1 = System.currentTimeMillis();
        List<SalesItemEntity> salesItems = createSalesItems(dto.getSalesItems());
        log.info("[TIMING] 상품조회: {}ms", System.currentTimeMillis() - t1);

        // 2. 주문 저장 (짧은 트랜잭션 - 상태: PENDING)
        long t2 = System.currentTimeMillis();
        SalesEntity savedSales = saveSalesInTransaction(orderId, salesItems);
        log.info("[TIMING] DB저장: {}ms", System.currentTimeMillis() - t2);

        // 3. Kafka 이벤트 발행 (order-created)
        long t3 = System.currentTimeMillis();
        publishOrderCreatedEvent(orderId, savedSales);
        log.info("[TIMING] Kafka발행: {}ms", System.currentTimeMillis() - t3);

        log.info("[TIMING] 전체: {}ms", System.currentTimeMillis() - startTime);
        log.info("[주문 생성 완료 - 비동기 처리 시작] orderId: {}, salesId: {}, 상품 {}건, 총액: {}",
                orderId, savedSales.getId(), savedSales.getItems().size(), savedSales.getTotalPrice());

        return ResSalesCreateDTO.from(savedSales);
    }

    /**
     * 주문 저장 + Outbox 저장 (같은 트랜잭션)
     */
    private SalesEntity saveSalesInTransaction(String orderId, List<SalesItemEntity> salesItems) {
        return transactionTemplate.execute(status -> {
            SalesEntity sales = SalesEntity.createWithItems(orderId, salesItems);
            SalesEntity savedSales = salesRepository.save(sales);

            // Outbox에 이벤트 저장 (같은 트랜잭션)
            saveToOutbox(orderId, savedSales);

            return savedSales;
        });
    }

    /**
     * Outbox 테이블에 이벤트 저장
     */
    private void saveToOutbox(String orderId, SalesEntity sales) {
        OrderCreatedEvent event = buildOrderCreatedEvent(orderId, sales);

        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEntity outbox = OutboxEntity.create(
                    "ORDER",
                    String.valueOf(sales.getId()),
                    "ORDER_CREATED",
                    payload,
                    TOPIC_ORDER_CREATED,
                    orderId
            );
            outboxRepository.save(outbox);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("이벤트 직렬화 실패", e);
        }
    }

    /**
     * 주문 생성 이벤트 발행 (AFTER_COMMIT)
     */
    private void publishOrderCreatedEvent(String orderId, SalesEntity sales) {
        OrderCreatedEvent event = buildOrderCreatedEvent(orderId, sales);
        orderEventProducer.publishOrderCreated(event, String.valueOf(sales.getId()));
    }

    private OrderCreatedEvent buildOrderCreatedEvent(String orderId, SalesEntity sales) {
        List<OrderCreatedEvent.OrderItem> items = sales.getItems().stream()
                .map(item -> OrderCreatedEvent.OrderItem.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .build())
                .collect(Collectors.toList());

        return OrderCreatedEvent.builder()
                .orderId(orderId)
                .salesId(sales.getId())
                .items(items)
                .totalAmount(sales.getTotalPrice())
                .createdAt(LocalDateTime.now())
                .build();
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

    @Override
    @Transactional
    public void completeSales(Long salesId, String orderId) {
        SalesEntity sales = salesRepository.findSalesBySalesId(salesId);

        if (sales == null) {
            log.warn("[주문 조회 실패] salesId: {} - 주문 없음", salesId);
            return;
        }

        // 이미 완료된 주문이면 스킵 (멱등성)
        if (sales.isCompleted()) {
            log.info("[주문 상태 변경 스킵 - 이미 완료] orderId: {}, salesId: {}", orderId, salesId);
            return;
        }

        sales.complete();
        log.info("[주문 상태 완료] orderId: {}, salesId: {}, status: {}",
                orderId, salesId, sales.getStatus());
    }

    @Override
    @Transactional
    public void cancelSales(Long salesId, String orderId, String reason) {
        SalesEntity sales = salesRepository.findSalesBySalesId(salesId);

        if (sales == null) {
            log.warn("[주문 조회 실패] salesId: {} - 주문 없음", salesId);
            return;
        }

        // 이미 취소된 주문이면 스킵 (멱등성)
        if (sales.isCancelled()) {
            log.info("[주문 상태 변경 스킵 - 이미 취소됨] orderId: {}, salesId: {}", orderId, salesId);
            return;
        }

        sales.cancel(reason);
        log.info("[주문 취소 완료] orderId: {}, salesId: {}, status: {}",
                orderId, salesId, sales.getStatus());
    }
}
