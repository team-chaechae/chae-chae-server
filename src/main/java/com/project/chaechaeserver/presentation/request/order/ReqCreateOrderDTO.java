package com.project.chaechaeserver.presentation.request.order;

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
public class ReqCreateOrderDTO {

  @Valid
  @NotNull
  private Order order;


  @Getter
  @Builder
  @AllArgsConstructor
  @NoArgsConstructor
  public static class Order {

    private Long productId;
    private Integer quantity;

  }
}
