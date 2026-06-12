package com.project.productservice.application.response;

import com.project.productservice.application.service.ProductPriceSnapshot;
import com.project.productservice.domain.model.ProductEntity;
import com.project.productservice.domain.model.constraint.PromotionType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
        return from(productPage, stockMap, Map.of());
    }

    public static ResProductSearchWithOrderStatusDTO from(Page<ProductEntity> productPage,
        Map<Long, Integer> stockMap, Map<Long, ProductPriceSnapshot> priceSnapshotMap) {
        return ResProductSearchWithOrderStatusDTO.builder()
            .productPage(ProductPage.from(productPage, stockMap, priceSnapshotMap))
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
            return from(productEntityPage, stockMap, Map.of());
        }

        public static ProductPage from(Page<ProductEntity> productEntityPage, Map<Long, Integer> stockMap,
            Map<Long, ProductPriceSnapshot> priceSnapshotMap) {
            return ProductPage.builder()
                .content(Products.from(productEntityPage.getContent(), stockMap, priceSnapshotMap))
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
            private Integer originalPrice;
            private boolean promotionApplied;
            private PromotionType promotionType;
            private Integer promotionPrice;
            private LocalDateTime promotionEndsAt;
            private Integer currentQuantity;

            public static List<Products> from(List<ProductEntity> productEntityList, java.util.Map<Long, Integer> stockMap) {
                return from(productEntityList, stockMap, Map.of());
            }

            public static List<Products> from(List<ProductEntity> productEntityList, Map<Long, Integer> stockMap,
                Map<Long, ProductPriceSnapshot> priceSnapshotMap) {
                return productEntityList.stream()
                    .map(product -> Products.from(product, stockMap, priceSnapshotMap))
                    .toList();
            }

            public static Products from(ProductEntity productEntity, java.util.Map<Long, Integer> stockMap) {
                return from(productEntity, stockMap, Map.of());
            }

            public static Products from(ProductEntity productEntity, Map<Long, Integer> stockMap,
                Map<Long, ProductPriceSnapshot> priceSnapshotMap) {
                ProductPriceSnapshot priceSnapshot = priceSnapshotMap.getOrDefault(
                    productEntity.getId(),
                    ProductPriceSnapshot.original(productEntity)
                );
                return Products.builder()
                    .id(productEntity.getId())
                    .name(productEntity.getName())
                    .category(productEntity.getCategory())
                    .price(priceSnapshot.getPrice())
                    .originalPrice(priceSnapshot.getOriginalPrice())
                    .promotionApplied(priceSnapshot.isPromotionApplied())
                    .promotionType(priceSnapshot.getPromotionType())
                    .promotionPrice(priceSnapshot.getPromotionPrice())
                    .promotionEndsAt(priceSnapshot.getPromotionEndsAt())
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
