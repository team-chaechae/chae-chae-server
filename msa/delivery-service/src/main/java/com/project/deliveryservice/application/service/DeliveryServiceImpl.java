package com.project.deliveryservice.application.service;

import com.project.deliveryservice.application.event.DeliveryStatusChangedInternalEvent;
import com.project.deliveryservice.application.global.dto.ResDeliveryDTO;
import com.project.deliveryservice.application.global.exception.BadRequestException;
import com.project.deliveryservice.application.global.exception.EntityNotFoundException;
import com.project.deliveryservice.domain.model.DeliveryCancellationRequestEntity;
import com.project.deliveryservice.domain.model.DeliveryEntity;
import com.project.deliveryservice.domain.model.constraint.DeliveryStatus;
import com.project.deliveryservice.domain.repository.DeliveryCancellationRequestRepository;
import com.project.deliveryservice.domain.repository.DeliveryRepository;
import com.project.deliveryservice.presentation.request.ReqAssignTrackingDTO;
import com.project.deliveryservice.presentation.request.ReqCreateDeliveryDTO;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryCancellationRequestRepository cancellationRequestRepository;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public ResDeliveryDTO createDelivery(ReqCreateDeliveryDTO request) {
        if (deliveryRepository.existsBySalesId(request.getSalesId())) {
            throw new BadRequestException("이미 생성된 배송 정보입니다.");
        }

        DeliveryEntity savedDelivery = saveNewDelivery(request);
        publishDeliveryStatusChanged(savedDelivery);
        log.info("delivery_created deliveryId={} salesId={} userId={}",
                savedDelivery.getId(), savedDelivery.getSalesId(), savedDelivery.getUserId());
        return ResDeliveryDTO.from(savedDelivery);
    }

    @Override
    @Transactional
    public ResDeliveryDTO createDeliveryIfAbsent(ReqCreateDeliveryDTO request) {
        return deliveryRepository.findBySalesId(request.getSalesId())
                .map(delivery -> {
                    log.info("delivery_create_requested_duplicate_skipped deliveryId={} salesId={}",
                            delivery.getId(), delivery.getSalesId());
                    return ResDeliveryDTO.from(delivery);
                })
                .orElseGet(() -> createDeliveryIdempotently(request));
    }

    @Override
    @Transactional
    public void createDeliveryFromEventIfAbsent(ReqCreateDeliveryDTO request) {
        if (cancellationRequestRepository.existsBySalesId(request.getSalesId())) {
            log.info("delivery_create_requested_skipped_cancel_requested salesId={} userId={}",
                    request.getSalesId(), request.getUserId());
            return;
        }
        createDeliveryIfAbsent(request);
    }

    private ResDeliveryDTO createDeliveryIdempotently(ReqCreateDeliveryDTO request) {
        DeliveryEntity savedDelivery = saveNewDelivery(request);
        publishDeliveryStatusChanged(savedDelivery);
        log.info("delivery_created_by_event deliveryId={} salesId={} userId={}",
                savedDelivery.getId(), savedDelivery.getSalesId(), savedDelivery.getUserId());
        return ResDeliveryDTO.from(savedDelivery);
    }

    private DeliveryEntity saveNewDelivery(ReqCreateDeliveryDTO request) {
        DeliveryEntity delivery = DeliveryEntity.create(
                request.getSalesId(),
                request.getUserId(),
                request.getRecipientName(),
                request.getRecipientPhone(),
                request.getZipCode(),
                request.getAddress(),
                request.getAddressDetail(),
                request.getDeliveryMemo()
        );
        return deliveryRepository.save(delivery);
    }

    @Override
    @Transactional(readOnly = true)
    public ResDeliveryDTO getDelivery(Long deliveryId) {
        return ResDeliveryDTO.from(findDelivery(deliveryId));
    }

    @Override
    @Transactional(readOnly = true)
    public ResDeliveryDTO getDeliveryBySalesId(Long salesId) {
        return deliveryRepository.findBySalesId(salesId)
                .map(ResDeliveryDTO::from)
                .orElseThrow(() -> new EntityNotFoundException("배송 정보를 찾을 수 없습니다."));
    }

    @Override
    @Transactional
    public ResDeliveryDTO assignTrackingNumber(Long deliveryId, ReqAssignTrackingDTO request) {
        DeliveryEntity delivery = findDeliveryForUpdate(deliveryId);
        if (deliveryRepository.existsByTrackingNumber(request.getTrackingNumber())) {
            throw new BadRequestException("이미 등록된 운송장 번호입니다.");
        }
        delivery.assignTrackingNumber(request.getTrackingNumber());
        log.info("delivery_tracking_assigned deliveryId={} salesId={} trackingNumber={}",
                delivery.getId(), delivery.getSalesId(), delivery.getTrackingNumber());
        return ResDeliveryDTO.from(delivery);
    }

    @Override
    @Transactional
    public ResDeliveryDTO shipDelivery(Long deliveryId) {
        DeliveryEntity delivery = findDeliveryForUpdate(deliveryId);
        delivery.ship(LocalDateTime.now(clock));
        publishDeliveryStatusChanged(delivery);
        log.info("delivery_shipped deliveryId={} salesId={} trackingNumber={}",
                delivery.getId(), delivery.getSalesId(), delivery.getTrackingNumber());
        return ResDeliveryDTO.from(delivery);
    }

    @Override
    @Transactional
    public ResDeliveryDTO completeDelivery(Long deliveryId) {
        DeliveryEntity delivery = findDeliveryForUpdate(deliveryId);
        delivery.complete(LocalDateTime.now(clock));
        publishDeliveryStatusChanged(delivery);
        log.info("delivery_completed deliveryId={} salesId={}", delivery.getId(), delivery.getSalesId());
        return ResDeliveryDTO.from(delivery);
    }

    @Override
    @Transactional
    public ResDeliveryDTO cancelDelivery(Long deliveryId) {
        DeliveryEntity delivery = findDeliveryForUpdate(deliveryId);
        delivery.cancel(LocalDateTime.now(clock));
        publishDeliveryStatusChanged(delivery);
        log.info("delivery_cancelled deliveryId={} salesId={}", delivery.getId(), delivery.getSalesId());
        return ResDeliveryDTO.from(delivery);
    }

    @Override
    @Transactional
    public void cancelDeliveryBySalesId(Long salesId, String orderId, String reason) {
        recordCancellationRequestIfAbsent(salesId, orderId, reason);

        deliveryRepository.findBySalesIdForUpdate(salesId)
                .ifPresentOrElse(
                        delivery -> cancelDeliveryForCompensation(delivery, orderId, reason),
                        () -> log.info("delivery_cancel_requested_recorded_without_delivery orderId={} salesId={}",
                                orderId, salesId)
                );
    }

    private void recordCancellationRequestIfAbsent(Long salesId, String orderId, String reason) {
        cancellationRequestRepository.findBySalesId(salesId)
                .orElseGet(() -> cancellationRequestRepository.save(
                        DeliveryCancellationRequestEntity.create(
                                salesId,
                                orderId,
                                reason,
                                LocalDateTime.now(clock)
                        )
                ));
    }

    private void cancelDeliveryForCompensation(DeliveryEntity delivery, String orderId, String reason) {
        if (delivery.getStatus() == DeliveryStatus.DELIVERED) {
            log.warn("delivery_cancel_requested_skipped_delivered orderId={} salesId={} deliveryId={} reason={}",
                    orderId, delivery.getSalesId(), delivery.getId(), reason);
            return;
        }
        if (delivery.getStatus() == DeliveryStatus.CANCELLED) {
            log.info("delivery_cancel_requested_duplicate_skipped orderId={} salesId={} deliveryId={}",
                    orderId, delivery.getSalesId(), delivery.getId());
            return;
        }
        delivery.cancel(LocalDateTime.now(clock));
        publishDeliveryStatusChanged(delivery);
        log.info("delivery_cancelled_by_compensation orderId={} salesId={} deliveryId={} reason={}",
                orderId, delivery.getSalesId(), delivery.getId(), reason);
    }

    private void publishDeliveryStatusChanged(DeliveryEntity delivery) {
        eventPublisher.publishEvent(DeliveryStatusChangedInternalEvent.from(delivery, LocalDateTime.now(clock)));
    }

    private DeliveryEntity findDelivery(Long deliveryId) {
        return deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new EntityNotFoundException("배송 정보를 찾을 수 없습니다."));
    }

    private DeliveryEntity findDeliveryForUpdate(Long deliveryId) {
        return deliveryRepository.findByIdForUpdate(deliveryId)
                .orElseThrow(() -> new EntityNotFoundException("배송 정보를 찾을 수 없습니다."));
    }
}
