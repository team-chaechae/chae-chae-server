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


    private Product product;

    public static ResCreateProductPostDTO from(ProductEntity productEntity) {
        return ResCreateProductPostDTO.builder()
            .product(Product.from(productEntity))
            .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Product {

        private Long id;
        private String name;
        private String category;
        private Integer price;
        private String unit;


        public static Product from(ProductEntity productEntity) {
            return Product.builder()
                .id(productEntity.getId())
                .name(productEntity.getName())
                .category(productEntity.getCategory())
                .price(productEntity.getPrice())
                .unit(productEntity.getUnit())
                .build();
        }
    }
}
