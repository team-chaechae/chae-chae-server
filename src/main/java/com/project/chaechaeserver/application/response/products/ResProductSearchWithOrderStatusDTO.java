package com.project.chaechaeserver.application.response.products;



import com.project.chaechaeserver.domain.model.products.ProductEntity;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;


@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ResProductSearchWithOrderStatusDTO {

    private ProductPage productPage;

    public static ResProductSearchWithOrderStatusDTO from(Page<ProductEntity> productPage, java.util.Map<Long, Integer> stockMap) {
        return ResProductSearchWithOrderStatusDTO.builder()
            .productPage(ProductPage.from(productPage, stockMap))
            .build();
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductPage {

        private List<Products> content;
        private ProductPageDetails page;

        public static ProductPage from(Page<ProductEntity> productEntityPage, java.util.Map<Long, Integer> stockMap) {
            return ProductPage.builder()
                .content(Products.from(productEntityPage.getContent(), stockMap))
                .page(ProductPageDetails.from(productEntityPage))
                .build();
        }

        @Builder
        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Products {

            private Long id;
            private String name;
            private String category;
            private Integer price;
            private Integer currentQuantity;

            public static List<Products> from(List<ProductEntity> productEntityList, java.util.Map<Long, Integer> stockMap) {
                return productEntityList.stream()
                    .map(product -> Products.from(product, stockMap))
                    .toList();
            }

            public static Products from(ProductEntity productEntity, java.util.Map<Long, Integer> stockMap) {
                return Products.builder()
                    .id(productEntity.getId())
                    .name(productEntity.getName())
                    .category(productEntity.getCategory())
                    .price(productEntity.getPrice())
                    .currentQuantity(stockMap.getOrDefault(productEntity.getId(), 0))
                    .build();
            }
        }

        @Builder
        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ProductPageDetails {

            private int size;
            private int number;
            private long totalElements;
            private int totalPages;


            public static ProductPageDetails from(Page<ProductEntity> pageDetails) {
                return ProductPageDetails.builder()
                    .size(pageDetails.getSize())
                    .number(pageDetails.getNumber())
                    .totalElements(pageDetails.getTotalElements())
                    .totalPages(pageDetails.getTotalPages())
                    .build();
            }
        }
    }

}
