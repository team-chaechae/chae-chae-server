package com.project.orderservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.orderservice.domain.model.SalesDeliveryStatusEntity;
import com.project.orderservice.domain.model.SalesDeliveryStatusType;
import com.project.orderservice.domain.repository.SalesDeliveryStatusRepository;
import com.project.orderservice.infrastructure.kafka.dto.DeliveryStatusChangedEvent;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class SalesDeliveryStatusProjectionServiceTest {

    private final SalesDeliveryStatusRepository repository = Mockito.mock(SalesDeliveryStatusRepository.class);
    private final SalesDeliveryStatusProjectionService service = new SalesDeliveryStatusProjectionService(repository);

    @Test
    void upsertCreatesProjectionWhenMissing() {
        DeliveryStatusChangedEvent event = event("event-1", SalesDeliveryStatusType.READY, time(10));
        when(repository.findBySalesId(7L)).thenReturn(Optional.empty());

        service.upsert(event);

        ArgumentCaptor<SalesDeliveryStatusEntity> captor = ArgumentCaptor.forClass(SalesDeliveryStatusEntity.class);
        verify(repository).save(captor.capture());
        SalesDeliveryStatusEntity saved = captor.getValue();
        assertThat(saved.getSalesId()).isEqualTo(7L);
        assertThat(saved.getDeliveryId()).isEqualTo(12L);
        assertThat(saved.getStatus()).isEqualTo(SalesDeliveryStatusType.READY);
        assertThat(saved.getLastEventId()).isEqualTo("event-1");
    }

    @Test
    void upsertIgnoresOlderEvent() {
        SalesDeliveryStatusEntity projection = SalesDeliveryStatusEntity.create(event(
                "event-new",
                SalesDeliveryStatusType.IN_TRANSIT,
                time(20)
        ));
        when(repository.findBySalesId(7L)).thenReturn(Optional.of(projection));

        service.upsert(event("event-old", SalesDeliveryStatusType.READY, time(10)));

        assertThat(projection.getStatus()).isEqualTo(SalesDeliveryStatusType.IN_TRANSIT);
        assertThat(projection.getLastEventId()).isEqualTo("event-new");
    }

    private DeliveryStatusChangedEvent event(
            String eventId,
            SalesDeliveryStatusType status,
            LocalDateTime occurredAt
    ) {
        return DeliveryStatusChangedEvent.builder()
                .eventId(eventId)
                .deliveryId(12L)
                .salesId(7L)
                .userId(3L)
                .status(status)
                .trackingNumber("TRACK-1")
                .occurredAt(occurredAt)
                .build();
    }

    private LocalDateTime time(int minute) {
        return LocalDateTime.of(2026, 6, 11, 15, minute);
    }
}
