package com.project.paymentservice.presentation.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReqTossPaymentCancelDTO {

    @Schema(example = "2339184", description = "판매 ID")
    @NotNull(message = "salesId는 필수입니다.")
    private Long salesId;

    @Schema(example = "고객 요청", description = "결제 취소 사유")
    @NotBlank(message = "cancelReason은 필수입니다.")
    @Size(max = 200, message = "cancelReason은 200자 이하여야 합니다.")
    private String cancelReason;
}
