package com.project.chaechaeserver.application.response.inventory;


import com.project.chaechaeserver.domain.model.inventory.InventoryEntity;
import com.project.chaechaeserver.domain.model.products.ProductEntity;
import com.project.chaechaeserver.domain.model.products.constraint.ProductStatusType;
import java.time.LocalDateTime;
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
public class ResInventorySearchDTO {

    private InventoryPage inventoryPage;

    public static ResInventorySearchDTO from(Page<InventoryEntity> inventoryPage) {
        return ResInventorySearchDTO.builder()
            .inventoryPage(InventoryPage.from(inventoryPage))
            .build();
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryPage {

        private List<Inventories> contents;
        private PageDetails page;

        private static InventoryPage from(Page<InventoryEntity> inventoryPage) {
            return InventoryPage.builder()
                .contents(Inventories.from(inventoryPage.getContent()))
                .page(PageDetails.from(inventoryPage))
                .build();
        }

        @Builder
        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Inventories {

            private Long inventoryId;
            private Long productId;
            private String productName;
            private Integer productPrice;
            private ProductStatusType productStatus;
            private Integer quantity;
            private LocalDateTime createdAt;
            private LocalDateTime updatedAt;

            public static List<Inventories> from(List<InventoryEntity> inventoryEntities) {
                return inventoryEntities.stream()
                    .map(Inventories::from)
                    .toList();
            }

            public static Inventories from(InventoryEntity inventoryEntity) {
                ProductEntity product = inventoryEntity.getProduct();
                return Inventories.builder()
                    .inventoryId(inventoryEntity.getId())
                    .productId(product.getId())
                    .productName(product.getName())
                    .productPrice(product.getPrice())
                    .productStatus(product.getProductStatusType())
                    .quantity(inventoryEntity.getQuantity())
                    .createdAt(inventoryEntity.getCreatedAt())
                    .updatedAt(inventoryEntity.getUpdatedAt())
                    .build();
            }
        }

        @Builder
        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class PageDetails {

            private int size;
            private int number;
            private long totalElements;
            private int totalPages;

            public static PageDetails from(Page<InventoryEntity> inventoryPage) {
                return PageDetails.builder()
                    .size(inventoryPage.getSize())
                    .number(inventoryPage.getNumber())
                    .totalElements(inventoryPage.getTotalElements())
                    .totalPages(inventoryPage.getTotalPages())
                    .build();
            }
        }
    }
}