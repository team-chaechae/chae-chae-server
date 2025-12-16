package com.project.paymentservice.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReqPaymentDTO {

    @Schema(example = "ord-uuid-12345", description = "주문 UUID")
    @NotBlank(message = "주문 ID는 필수입니다.")
    private String orderId;

    @Schema(example = "101", description = "판매 ID")
    @NotNull(message = "판매 ID는 필수입니다.")
    private Long salesId;

    @Schema(example = "50000", description = "결제 금액")
    @NotNull(message = "결제 금액은 필수입니다.")
    @Positive(message = "결제 금액은 양수여야 합니다.")
    private Integer amount;
}
