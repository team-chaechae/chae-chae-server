package com.project.chaechaeserver.application.response.inventory;


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

    public static ResInventorySearchDTO fromDto(Page<InventoryWithProductDto> inventoryPage) {
        return ResInventorySearchDTO.builder()
            .inventoryPage(InventoryPage.fromDto(inventoryPage))
            .build();
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryPage {

        private List<Inventories> contents;
        private PageDetails page;

        private static InventoryPage fromDto(Page<InventoryWithProductDto> inventoryPage) {
            return InventoryPage.builder()
                .contents(Inventories.fromDto(inventoryPage.getContent()))
                .page(PageDetails.fromDto(inventoryPage))
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

            public static List<Inventories> fromDto(List<InventoryWithProductDto> dtoList) {
                return dtoList.stream()
                    .map(Inventories::fromDto)
                    .toList();
            }

            public static Inventories fromDto(InventoryWithProductDto dto) {
                return Inventories.builder()
                    .inventoryId(dto.getInventoryId())
                    .productId(dto.getProductId())
                    .productName(dto.getProductName())
                    .productPrice(dto.getProductPrice())
                    .productStatus(dto.getProductStatus())
                    .quantity(dto.getQuantity())
                    .createdAt(dto.getCreatedAt())
                    .updatedAt(dto.getUpdatedAt())
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

            public static PageDetails fromDto(Page<InventoryWithProductDto> inventoryPage) {
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