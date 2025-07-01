package com.project.chaechaeserver.application.response.products;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResCreateProductInfoDTO {


    private ProductInfo productInfo;

    public static ResCreateProductInfoDTO of(Long id ,String name, String category, int price, String unit) {
        return ResCreateProductInfoDTO.builder()
            .productInfo(ProductInfo.from(id,name, category, price, unit))
            .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductInfo {

        private Long id;
        private String name;
        private String category;
        private int price;
        private String unit;


        public static ProductInfo from(Long id, String name, String category, int price, String unit) {
            return ProductInfo.builder()
                .id(id)
                .name(name)
                .category(category)
                .price(price)
                .unit(unit)
                .build();

        }
    }
}
