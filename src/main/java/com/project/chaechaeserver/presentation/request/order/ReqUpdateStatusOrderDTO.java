package com.project.chaechaeserver.presentation.request.order;

import com.project.chaechaeserver.domain.model.order.constraint.StatusType;
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
    private StatusType status;
}
