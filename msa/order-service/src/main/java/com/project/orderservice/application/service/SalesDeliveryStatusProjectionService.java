package com.project.orderservice.application.service;

import com.project.orderservice.domain.model.SalesDeliveryStatusEntity;
import com.project.orderservice.domain.repository.SalesDeliveryStatusRepository;
import com.project.orderservice.infrastructure.kafka.dto.DeliveryStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesDeliveryStatusProjectionService {

    private final SalesDeliveryStatusRepository salesDeliveryStatusRepository;

    @Transactional
    public void upsert(DeliveryStatusChangedEvent event) {
        salesDeliveryStatusRepository.findBySalesId(event.getSalesId())
                .ifPresentOrElse(
                        projection -> projection.applyIfNewer(event),
                        () -> salesDeliveryStatusRepository.save(SalesDeliveryStatusEntity.create(event))
                );

        log.info("sales_delivery_status_projection_upserted salesId={} deliveryId={} status={} eventId={}",
                event.getSalesId(), event.getDeliveryId(), event.getStatus(), event.getEventId());
    }
}
