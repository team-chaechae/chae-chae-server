package com.project.orderservice.application.service;

import com.project.orderservice.application.global.exception.BadRequestException;
import com.project.orderservice.application.global.exception.PaymentFailedException;
import com.project.orderservice.application.response.ResSalesCreateDTO;
import com.project.orderservice.application.response.ResSalesGetByIdDTO;
import com.project.orderservice.application.response.ResSalesSearchDTO;
import com.project.orderservice.domain.model.SalesEntity;
import com.project.orderservice.domain.model.SalesItemEntity;
import com.project.orderservice.domain.repository.SalesRepository;
import com.project.orderservice.infrastructure.client.InventoryClient;
import com.project.orderservice.infrastructure.client.PaymentClient;
import com.project.orderservice.infrastructure.client.ProductCacheClient;
import com.project.orderservice.infrastructure.client.dto.PaymentDTO;
import com.project.orderservice.infrastructure.client.dto.ProductDTO;
import com.project.orderservice.infrastructure.client.dto.StockReservationDTO;
import com.project.orderservice.presentation.request.ReqCreateSalesDTO;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesServiceImpl implements SalesService {

    private final SalesRepository salesRepository;
    private final ProductCacheClient productCacheClient;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;

    @Override
    @Transactional
    public ResSalesCreateDTO createSales(ReqCreateSalesDTO dto) {
        String orderId = UUID.randomUUID().toString();

        // 1. 주문 아이템 생성 및 상품 정보 조회
        List<SalesItemEntity> salesItems = createSalesItems(dto.getSalesItems());

        // 2. 주문 엔티티 생성 (PENDING 상태)
        SalesEntity sales = SalesEntity.createWithItems(salesItems);
        SalesEntity savedSales = salesRepository.save(sales);

        log.info("[주문 생성 시작] orderId: {}, salesId: {}, 상품 {}건, 총액: {}",
                orderId, savedSales.getId(), savedSales.getItems().size(), savedSales.getTotalPrice());

        try {
            // 3. 재고 예약 (동기 호출)
            StockReservationDTO.ReserveResponse reserveResponse = reserveStock(orderId, savedSales);

            if (!reserveResponse.isSuccess()) {
                log.warn("[재고 예약 실패] orderId: {}, salesId: {}, 사유: {}",
                        orderId, savedSales.getId(), reserveResponse.getFailureReason());
                savedSales.cancel("재고 부족: " + reserveResponse.getFailureReason());
                throw new BadRequestException("재고 예약 실패: " + reserveResponse.getFailureReason());
            }

            log.info("[재고 예약 완료] orderId: {}, salesId: {}", orderId, savedSales.getId());

            // 4. 결제 처리 (동기 호출) - 결제 완료 후 Payment Service에서 이벤트 발행
            PaymentDTO.Response paymentResponse;
            try {
                paymentResponse = processPayment(orderId, savedSales);
                log.info("[결제 요청 완료] orderId: {}, salesId: {}, paymentId: {}",
                        orderId, savedSales.getId(), paymentResponse.getPayment().getId());
            } catch (Exception e) {
                // 결제 실패 시 재고 예약 해제
                log.error("[결제 실패] orderId: {}, salesId: {}, 에러: {}. 재고 해제 시작",
                        orderId, savedSales.getId(), e.getMessage());
                releaseStock(orderId, savedSales, "결제 실패: " + e.getMessage());
                savedSales.cancel("결제 실패: " + e.getMessage());
                throw new PaymentFailedException("결제 처리 실패: " + e.getMessage());
            }

            // 5. 주문 상태는 PENDING 유지 - Payment Service에서 발행한 이벤트로 완료 처리됨
            log.info("[주문 생성 완료 - 이벤트 대기] orderId: {}, salesId: {}, 결제 상태: {}",
                    orderId, savedSales.getId(), paymentResponse.getPayment().getStatus());

            return ResSalesCreateDTO.from(savedSales);

        } catch (BadRequestException | PaymentFailedException e) {
            throw e;
        } catch (FeignException e) {
            log.error("[서비스 호출 실패] orderId: {}, salesId: {}, 에러: {}",
                    orderId, savedSales.getId(), e.getMessage());
            savedSales.cancel("서비스 호출 실패: " + e.getMessage());
            throw new BadRequestException("주문 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * 재고 예약
     */
    private StockReservationDTO.ReserveResponse reserveStock(String orderId, SalesEntity sales) {
        List<StockReservationDTO.ReserveRequest.ReserveItem> items = sales.getItems().stream()
                .map(item -> StockReservationDTO.ReserveRequest.ReserveItem.builder()
                        .productId(item.getProductId())
                        .quantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());

        StockReservationDTO.ReserveRequest request = StockReservationDTO.ReserveRequest.builder()
                .orderId(orderId)
                .salesId(sales.getId())
                .items(items)
                .build();

        return inventoryClient.reserveStock(request);
    }

    /**
     * 결제 처리
     */
    private PaymentDTO.Response processPayment(String orderId, SalesEntity sales) {
        PaymentDTO.Request request = PaymentDTO.Request.builder()
                .orderId(orderId)
                .salesId(sales.getId())
                .amount(sales.getTotalPrice())
                .build();

        return paymentClient.processPayment(request);
    }

    /**
     * 재고 예약 해제 (보상 트랜잭션)
     */
    private void releaseStock(String orderId, SalesEntity sales, String reason) {
        try {
            List<StockReservationDTO.ReleaseRequest.ReleaseItem> items = sales.getItems().stream()
                    .map(item -> StockReservationDTO.ReleaseRequest.ReleaseItem.builder()
                            .productId(item.getProductId())
                            .build())
                    .collect(Collectors.toList());

            StockReservationDTO.ReleaseRequest request = StockReservationDTO.ReleaseRequest.builder()
                    .orderId(orderId)
                    .salesId(sales.getId())
                    .items(items)
                    .reason(reason)
                    .build();

            StockReservationDTO.ReleaseResponse response = inventoryClient.releaseStock(request);

            if (response.isSuccess()) {
                log.info("[재고 해제 완료] orderId: {}, salesId: {}", orderId, sales.getId());
            } else {
                log.error("[재고 해제 실패] orderId: {}, salesId: {}, 사유: {}",
                        orderId, sales.getId(), response.getMessage());
            }
        } catch (Exception e) {
            log.error("[재고 해제 중 에러] orderId: {}, salesId: {}, 에러: {}",
                    orderId, sales.getId(), e.getMessage());
        }
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
}
