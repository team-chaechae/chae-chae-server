package com.project.orderservice.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryAddressSnapshot {

    @Column(name = "recipient_name", length = 80)
    private String recipientName;

    @Column(name = "recipient_phone", length = 30)
    private String recipientPhone;

    @Column(name = "zip_code", length = 20)
    private String zipCode;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "address_detail", length = 255)
    private String addressDetail;

    @Column(name = "delivery_memo", length = 255)
    private String deliveryMemo;

    private DeliveryAddressSnapshot(
            String recipientName,
            String recipientPhone,
            String zipCode,
            String address,
            String addressDetail,
            String deliveryMemo
    ) {
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.zipCode = zipCode;
        this.address = address;
        this.addressDetail = addressDetail;
        this.deliveryMemo = deliveryMemo;
    }

    public static DeliveryAddressSnapshot create(
            String recipientName,
            String recipientPhone,
            String zipCode,
            String address,
            String addressDetail,
            String deliveryMemo
    ) {
        return new DeliveryAddressSnapshot(
                recipientName,
                recipientPhone,
                zipCode,
                address,
                addressDetail,
                deliveryMemo
        );
    }
}
