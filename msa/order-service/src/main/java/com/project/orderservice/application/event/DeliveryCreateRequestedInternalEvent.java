package com.project.orderservice.application.event;

import com.project.orderservice.domain.model.DeliveryAddressSnapshot;
import com.project.orderservice.domain.model.SalesEntity;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeliveryCreateRequestedInternalEvent {

    private final String eventId;
    private final String orderId;
    private final Long salesId;
    private final Long userId;
    private final String recipientName;
    private final String recipientPhone;
    private final String zipCode;
    private final String address;
    private final String addressDetail;
    private final String deliveryMemo;
    private final LocalDateTime requestedAt;

    public String getAggregateId() {
        return String.valueOf(salesId);
    }

    public String getMessageKey() {
        return orderId;
    }

    public static DeliveryCreateRequestedInternalEvent from(SalesEntity sales) {
        DeliveryAddressSnapshot deliveryAddress = sales.getDeliveryAddress();
        return of(
                sales.getOrderId(),
                sales.getId(),
                sales.getUserId(),
                deliveryAddress.getRecipientName(),
                deliveryAddress.getRecipientPhone(),
                deliveryAddress.getZipCode(),
                deliveryAddress.getAddress(),
                deliveryAddress.getAddressDetail(),
                deliveryAddress.getDeliveryMemo()
        );
    }

    public static DeliveryCreateRequestedInternalEvent of(
            String orderId,
            Long salesId,
            Long userId,
            String recipientName,
            String recipientPhone,
            String zipCode,
            String address,
            String addressDetail,
            String deliveryMemo
    ) {
        return DeliveryCreateRequestedInternalEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(orderId)
                .salesId(salesId)
                .userId(userId)
                .recipientName(recipientName)
                .recipientPhone(recipientPhone)
                .zipCode(zipCode)
                .address(address)
                .addressDetail(addressDetail)
                .deliveryMemo(deliveryMemo)
                .requestedAt(LocalDateTime.now())
                .build();
    }
}
