package com.project.deliveryservice.application.service;

import com.project.deliveryservice.application.global.dto.ResDeliveryDTO;
import com.project.deliveryservice.application.global.exception.BadRequestException;
import com.project.deliveryservice.application.global.exception.EntityNotFoundException;
import com.project.deliveryservice.domain.model.DeliveryEntity;
import com.project.deliveryservice.domain.repository.DeliveryRepository;
import com.project.deliveryservice.presentation.request.ReqAssignTrackingDTO;
import com.project.deliveryservice.presentation.request.ReqCreateDeliveryDTO;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final Clock clock;

    @Override
    @Transactional
    public ResDeliveryDTO createDelivery(ReqCreateDeliveryDTO request) {
        if (deliveryRepository.existsBySalesId(request.getSalesId())) {
            throw new BadRequestException("이미 생성된 배송 정보입니다.");
        }

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
        DeliveryEntity savedDelivery = deliveryRepository.save(delivery);
        log.info("delivery_created deliveryId={} salesId={} userId={}",
                savedDelivery.getId(), savedDelivery.getSalesId(), savedDelivery.getUserId());
        return ResDeliveryDTO.from(savedDelivery);
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
        log.info("delivery_shipped deliveryId={} salesId={} trackingNumber={}",
                delivery.getId(), delivery.getSalesId(), delivery.getTrackingNumber());
        return ResDeliveryDTO.from(delivery);
    }

    @Override
    @Transactional
    public ResDeliveryDTO completeDelivery(Long deliveryId) {
        DeliveryEntity delivery = findDeliveryForUpdate(deliveryId);
        delivery.complete(LocalDateTime.now(clock));
        log.info("delivery_completed deliveryId={} salesId={}", delivery.getId(), delivery.getSalesId());
        return ResDeliveryDTO.from(delivery);
    }

    @Override
    @Transactional
    public ResDeliveryDTO cancelDelivery(Long deliveryId) {
        DeliveryEntity delivery = findDeliveryForUpdate(deliveryId);
        delivery.cancel(LocalDateTime.now(clock));
        log.info("delivery_cancelled deliveryId={} salesId={}", delivery.getId(), delivery.getSalesId());
        return ResDeliveryDTO.from(delivery);
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
