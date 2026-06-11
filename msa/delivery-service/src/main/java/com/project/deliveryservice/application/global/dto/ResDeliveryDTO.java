package com.project.deliveryservice.application.global.dto;

import com.project.deliveryservice.domain.model.DeliveryEntity;
import com.project.deliveryservice.domain.model.constraint.DeliveryStatus;
import java.time.LocalDateTime;

public record ResDeliveryDTO(
        Long id,
        Long salesId,
        Long userId,
        String recipientName,
        String recipientPhone,
        String zipCode,
        String address,
        String addressDetail,
        String deliveryMemo,
        String trackingNumber,
        DeliveryStatus status,
        LocalDateTime shippedAt,
        LocalDateTime deliveredAt,
        LocalDateTime cancelledAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ResDeliveryDTO from(DeliveryEntity delivery) {
        return new ResDeliveryDTO(
                delivery.getId(),
                delivery.getSalesId(),
                delivery.getUserId(),
                delivery.getRecipientName(),
                delivery.getRecipientPhone(),
                delivery.getZipCode(),
                delivery.getAddress(),
                delivery.getAddressDetail(),
                delivery.getDeliveryMemo(),
                delivery.getTrackingNumber(),
                delivery.getStatus(),
                delivery.getShippedAt(),
                delivery.getDeliveredAt(),
                delivery.getCancelledAt(),
                delivery.getCreatedAt(),
                delivery.getUpdatedAt()
        );
    }
}
