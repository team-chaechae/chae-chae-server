package com.project.orderservice.application.response;

import com.project.orderservice.domain.model.SalesEntity;
import io.swagger.v3.oas.annotations.media.Schema;
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

        private List<SalesSummary> content;
        private PageDetails page;

        public static SalesPage from(Page<SalesEntity> salesEntityPage) {
            return SalesPage.builder()
                    .content(SalesSummary.from(salesEntityPage.getContent()))
                    .page(PageDetails.from(salesEntityPage))
                    .build();
        }

        @Getter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class SalesSummary {

            @Schema(example = "101")
            private Long salesId;

            @Schema(example = "COMPLETED")
            private String status;

            @Schema(example = "3")
            private int itemCount;

            @Schema(example = "10")
            private int totalQuantity;

            @Schema(example = "45000")
            private int totalPrice;

            private LocalDateTime createdAt;

            public static List<SalesSummary> from(List<SalesEntity> salesEntityList) {
                return salesEntityList.stream()
                        .map(SalesSummary::from)
                        .toList();
            }

            public static SalesSummary from(SalesEntity sales) {
                return SalesSummary.builder()
                        .salesId(sales.getId())
                        .status(sales.getStatus().name())
                        .itemCount(sales.getItems().size())
                        .totalQuantity(sales.getTotalQuantity())
                        .totalPrice(sales.getTotalPrice())
                        .createdAt(sales.getCreatedAt())
                        .build();
            }
        }

        @Getter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class PageDetails {

            @Schema(example = "10")
            private int size;

            @Schema(example = "0")
            private int number;

            @Schema(example = "25")
            private long totalElements;

            @Schema(example = "3")
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
