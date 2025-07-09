package com.project.chaechaeserver.application.response.sales;

import com.project.chaechaeserver.domain.model.sales.SalesEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResSalesSearchDTO {

    private SalesPage salesPage;

    public static ResSalesSearchDTO from(Page<SalesEntity> salesEntityPage) {
        return ResSalesSearchDTO.builder()
                .salesPage(SalesPage.from(salesEntityPage))
                .build();
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalesPage {

        private List<Sales> content;
        private PageDetails page;

        public static SalesPage from(Page<SalesEntity> salesEntityPage) {
            return SalesPage.builder()
                    .content(Sales.from(salesEntityPage.getContent()))
                    .page(PageDetails.from(salesEntityPage))
                    .build();
        }

        @Getter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Sales {

            private Long salesId;
            private Long productId;
            private String productName;
            private int quantity;
            private int price;
            private int totalPrice;
            private LocalDateTime createdAt;

            public static List<Sales> from(List<SalesEntity> salesEntityList) {
                return salesEntityList.stream()
                        .map(Sales::from)
                        .toList();
            }

            public static Sales from(SalesEntity salesEntity) {

                int quantity = salesEntity.getQuantity();
                int price = salesEntity.getPrice();

                return Sales.builder()
                        .salesId(salesEntity.getId())
                        .productId(salesEntity.getProductEntity().getId())
                        .productName(salesEntity.getProductEntity().getName())
                        .quantity(quantity)
                        .price(price)
                        .totalPrice(price * quantity)
                        .createdAt(salesEntity.getCreatedAt())
                        .build();
            }
        }

        @Getter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class PageDetails {

            private int size;
            private int number;
            private long totalElements;
            private int totalPages;

            public static PageDetails from(Page<SalesEntity> salesEntityPage) {
                return PageDetails.builder()
                        .size(salesEntityPage.getSize())
                        .number(salesEntityPage.getNumber())
                        .totalElements(salesEntityPage.getTotalElements())
                        .totalPages(salesEntityPage.getTotalPages())
                        .build();
            }
        }
    }
}
