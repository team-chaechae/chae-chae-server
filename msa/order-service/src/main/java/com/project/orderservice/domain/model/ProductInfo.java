package com.project.orderservice.domain.model;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class ProductInfo {

  private Long productId;
  private String productName;
  private String productCategory;
  private Integer productPrice;

  public ProductInfo(Long productId, String productName, String productCategory, Integer productPrice) {
    this.productId = productId;
    this.productName = productName;
    this.productCategory = productCategory;
    this.productPrice = productPrice;
  }

}
