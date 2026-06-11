package com.project.deliveryservice.presentation.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReqCreateDeliveryDTO {

    @NotNull
    @Min(1)
    private Long salesId;

    @NotNull
    @Min(1)
    private Long userId;

    @NotBlank
    @Size(max = 80)
    private String recipientName;

    @NotBlank
    @Size(max = 30)
    private String recipientPhone;

    @NotBlank
    @Size(max = 20)
    private String zipCode;

    @NotBlank
    @Size(max = 255)
    private String address;

    @Size(max = 255)
    private String addressDetail;

    @Size(max = 255)
    private String deliveryMemo;
}
