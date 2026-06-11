package com.project.deliveryservice.domain.model;

import com.project.deliveryservice.application.global.exception.BadRequestException;
import com.project.deliveryservice.domain.model.constraint.DeliveryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(
        name = "delivery",
        indexes = {
                @Index(name = "idx_delivery_sales_id", columnList = "sales_id", unique = true),
                @Index(name = "idx_delivery_status", columnList = "status"),
                @Index(name = "idx_delivery_tracking_number", columnList = "tracking_number", unique = true)
        }
)
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sales_id", nullable = false, unique = true)
    private Long salesId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "recipient_name", nullable = false, length = 80)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false, length = 30)
    private String recipientPhone;

    @Column(name = "zip_code", nullable = false, length = 20)
    private String zipCode;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(name = "address_detail", length = 255)
    private String addressDetail;

    @Column(name = "delivery_memo", length = 255)
    private String deliveryMemo;

    @Column(name = "tracking_number", unique = true, length = 80)
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryStatus status;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private DeliveryEntity(
            Long salesId,
            Long userId,
            String recipientName,
            String recipientPhone,
            String zipCode,
            String address,
            String addressDetail,
            String deliveryMemo
    ) {
        validateRequired(salesId, userId, recipientName, recipientPhone, zipCode, address);
        this.salesId = salesId;
        this.userId = userId;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.zipCode = zipCode;
        this.address = address;
        this.addressDetail = addressDetail;
        this.deliveryMemo = deliveryMemo;
        this.status = DeliveryStatus.READY;
    }

    public static DeliveryEntity create(
            Long salesId,
            Long userId,
            String recipientName,
            String recipientPhone,
            String zipCode,
            String address,
            String addressDetail,
            String deliveryMemo
    ) {
        return new DeliveryEntity(salesId, userId, recipientName, recipientPhone, zipCode, address, addressDetail, deliveryMemo);
    }

    public void assignTrackingNumber(String trackingNumber) {
        if (status != DeliveryStatus.READY) {
            throw new BadRequestException("배송 준비 상태에서만 운송장을 등록할 수 있습니다.");
        }
        if (trackingNumber == null || trackingNumber.isBlank()) {
            throw new BadRequestException("운송장 번호는 필수입니다.");
        }
        this.trackingNumber = trackingNumber;
    }

    public void ship(LocalDateTime now) {
        if (status != DeliveryStatus.READY) {
            throw new BadRequestException("배송 준비 상태에서만 출고할 수 있습니다.");
        }
        if (trackingNumber == null || trackingNumber.isBlank()) {
            throw new BadRequestException("운송장 등록 후 출고할 수 있습니다.");
        }
        this.status = DeliveryStatus.IN_TRANSIT;
        this.shippedAt = now;
    }

    public void complete(LocalDateTime now) {
        if (status != DeliveryStatus.IN_TRANSIT) {
            throw new BadRequestException("배송 중 상태에서만 배송 완료할 수 있습니다.");
        }
        this.status = DeliveryStatus.DELIVERED;
        this.deliveredAt = now;
    }

    public void cancel(LocalDateTime now) {
        if (status == DeliveryStatus.DELIVERED) {
            throw new BadRequestException("배송 완료 건은 취소할 수 없습니다.");
        }
        if (status == DeliveryStatus.CANCELLED) {
            return;
        }
        this.status = DeliveryStatus.CANCELLED;
        this.cancelledAt = now;
    }

    private static void validateRequired(
            Long salesId,
            Long userId,
            String recipientName,
            String recipientPhone,
            String zipCode,
            String address
    ) {
        if (salesId == null || salesId < 1) {
            throw new BadRequestException("판매 ID는 필수입니다.");
        }
        if (userId == null || userId < 1) {
            throw new BadRequestException("사용자 ID는 필수입니다.");
        }
        if (recipientName == null || recipientName.isBlank()) {
            throw new BadRequestException("수령인 이름은 필수입니다.");
        }
        if (recipientPhone == null || recipientPhone.isBlank()) {
            throw new BadRequestException("수령인 연락처는 필수입니다.");
        }
        if (zipCode == null || zipCode.isBlank()) {
            throw new BadRequestException("우편번호는 필수입니다.");
        }
        if (address == null || address.isBlank()) {
            throw new BadRequestException("주소는 필수입니다.");
        }
    }
}
