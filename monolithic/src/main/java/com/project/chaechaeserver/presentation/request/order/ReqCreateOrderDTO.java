package com.project.chaechaeserver.presentation.request.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReqCreateOrderDTO {

  @Valid
  @NotNull(message = "발주를 위한 정보를 입력해주세요.")
  private Order order;


  @Getter
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Order {

    @Schema(example = "1")
    private Long productId;

    @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
    @Schema(example = "10")
    private Integer quantity;

  }
}
