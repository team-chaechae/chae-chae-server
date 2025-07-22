package com.project.chaechaeserver.application.response.products;




import com.project.chaechaeserver.domain.model.products.ProductEntity;
import java.time.LocalDateTime;
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
        private String status;
        private String orderStatus;
        private LocalDateTime createAt;
        private LocalDateTime updatedAt;


        public static Product from(ProductEntity productEntity) {
            return Product.builder()
                .id(productEntity.getId())
                .name(productEntity.getName())
                .category(productEntity.getCategory())
                .price(productEntity.getPrice())
                .status("PENDING")
                .orderStatus(null)
                .createAt(productEntity.getCreatedAt())
                .updatedAt(productEntity.getUpdatedAt())
                .build();
        }

    }
}
