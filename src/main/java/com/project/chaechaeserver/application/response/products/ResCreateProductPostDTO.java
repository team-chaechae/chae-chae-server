package com.project.chaechaeserver.application.response.products;


import com.project.chaechaeserver.domain.model.products.ProductEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResCreateProductPostDTO {


    private ProductInfo productInfo;

    public static ResCreateProductPostDTO from(ProductEntity productEntity) {
        return ResCreateProductPostDTO.builder()
            .productInfo(ProductInfo.from(productEntity))
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


        public static ProductInfo from(ProductEntity productEntity) {
            return ProductInfo.builder()
                .id(productEntity.getId())
                .name(productEntity.getName())
                .category(productEntity.getCategory())
                .price(productEntity.getPrice())
                .unit(productEntity.getUnit())
                .build();
        }
    }
}
