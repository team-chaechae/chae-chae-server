package com.project.couponservice.presentation.request;

import com.project.couponservice.domain.model.constraint.DiscountType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReqCreateCouponPolicyDTO {

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Size(max = 80)
    private String code;

    @NotNull
    private DiscountType discountType;

    @Min(1)
    private int discountValue;

    @Min(0)
    private int minOrderAmount;

    @Min(1)
    private Integer maxDiscountAmount;

    @Min(1)
    private Integer totalQuantity;

    @NotNull
    private LocalDateTime startsAt;

    @NotNull
    private LocalDateTime endsAt;
}
