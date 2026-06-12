package com.project.deliveryservice.application.event.listener;

import com.project.deliveryservice.application.event.DeliveryStatusChangedInternalEvent;
import com.project.deliveryservice.application.event.service.DeliveryEventSendService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "delivery.events.kafka-publish-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class DeliveryEventPublishListener {

    private final DeliveryEventSendService deliveryEventSendService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishDeliveryStatusChanged(DeliveryStatusChangedInternalEvent event) {
        deliveryEventSendService.sendDeliveryStatusChanged(event);
    }
}
