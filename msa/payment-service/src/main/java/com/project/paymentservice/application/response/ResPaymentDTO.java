package com.project.paymentservice.application.response;

import com.project.paymentservice.domain.model.PaymentEntity;
import com.project.paymentservice.domain.model.PaymentHistoryEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResPaymentDTO {

    private PaymentDetail payment;

    public static ResPaymentDTO from(PaymentEntity payment) {
        return ResPaymentDTO.builder()
                .payment(PaymentDetail.from(payment))
                .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentDetail {

        @Schema(example = "1")
        private Long paymentId;

        @Schema(example = "101")
        private Long salesId;

        @Schema(example = "50000")
        private Integer amount;

        @Schema(example = "COMPLETED")
        private String status;

        private String failureReason;

        private String tossPaymentKey;

        private String paymentMethod;

        private LocalDateTime approvedAt;

        private List<HistoryDetail> histories;

        private LocalDateTime createdAt;

        private LocalDateTime updatedAt;

        public static PaymentDetail from(PaymentEntity payment) {
            List<HistoryDetail> historyDetails = payment.getHistories().stream()
                    .map(HistoryDetail::from)
                    .toList();

            return PaymentDetail.builder()
                    .paymentId(payment.getId())
                    .salesId(payment.getSalesId())
                    .amount(payment.getAmount())
                    .status(payment.getStatus().name())
                    .failureReason(payment.getFailureReason())
                    .tossPaymentKey(payment.getTossPaymentKey())
                    .paymentMethod(payment.getPaymentMethod())
                    .approvedAt(payment.getApprovedAt())
                    .histories(historyDetails)
                    .createdAt(payment.getCreatedAt())
                    .updatedAt(payment.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryDetail {

        private Long historyId;

        private String status;

        private String description;

        private LocalDateTime createdAt;

        public static HistoryDetail from(PaymentHistoryEntity history) {
            return HistoryDetail.builder()
                    .historyId(history.getId())
                    .status(history.getStatus().name())
                    .description(history.getDescription())
                    .createdAt(history.getCreatedAt())
                    .build();
        }
    }
}
