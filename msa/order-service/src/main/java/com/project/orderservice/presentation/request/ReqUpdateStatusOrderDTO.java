package com.project.orderservice.presentation.request;

import com.project.orderservice.domain.model.constraint.StatusType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReqUpdateStatusOrderDTO {

    @Valid
    @NotNull(message = "변경할 발주 상태를 입력해주세요.")
    @Schema(example = "COMPLETED")
    private StatusType status;
}
