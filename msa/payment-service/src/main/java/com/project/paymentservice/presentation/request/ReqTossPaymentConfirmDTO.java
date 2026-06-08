package com.project.paymentservice.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReqTossPaymentConfirmDTO {

    @Schema(example = "tgen_20260519123456AbCdE", description = "토스페이먼츠 paymentKey")
    @NotBlank(message = "paymentKey는 필수입니다.")
    @Size(max = 200, message = "paymentKey는 200자 이하여야 합니다.")
    private String paymentKey;

    @Schema(example = "ord-uuid-12345", description = "주문 UUID")
    @NotBlank(message = "주문 ID는 필수입니다.")
    @Size(min = 6, max = 64, message = "주문 ID는 6자 이상 64자 이하여야 합니다.")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "주문 ID는 영문, 숫자, 하이픈, 언더스코어만 사용할 수 있습니다.")
    private String orderId;

    @Schema(example = "101", description = "판매 ID")
    @NotNull(message = "판매 ID는 필수입니다.")
    private Long salesId;

    @Schema(example = "50000", description = "결제 금액")
    @NotNull(message = "결제 금액은 필수입니다.")
    @Positive(message = "결제 금액은 양수여야 합니다.")
    private Integer amount;
}
