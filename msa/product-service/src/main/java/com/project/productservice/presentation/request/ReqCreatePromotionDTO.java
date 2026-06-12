package com.project.productservice.presentation.request;

import com.project.productservice.domain.model.constraint.PromotionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReqCreatePromotionDTO {

    @NotNull(message = "프로모션 타입은 필수입니다.")
    private PromotionType promotionType;

    @NotNull(message = "프로모션 가격은 필수입니다.")
    @Positive(message = "프로모션 가격은 0보다 커야 합니다.")
    private Integer promotionPrice;

    @NotNull(message = "프로모션 시작 시각은 필수입니다.")
    private LocalDateTime startsAt;

    @NotNull(message = "프로모션 종료 시각은 필수입니다.")
    private LocalDateTime endsAt;
}
