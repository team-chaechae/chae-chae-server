package com.project.couponservice.presentation.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReqValidateCouponDTO {

    @NotNull
    @Min(1)
    private Long userId;

    @Min(1)
    private int orderAmount;
}
