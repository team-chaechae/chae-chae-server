package com.project.orderservice.application.response;

import com.project.orderservice.domain.model.SalesDeliveryStatusEntity;
import com.project.orderservice.domain.model.SalesEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResSalesSearchDTO {

    private SalesPage salesPage;

    public static ResSalesSearchDTO from(Page<SalesEntity> salesEntityPage) {
        return from(salesEntityPage, Map.of());
    }

    public static ResSalesSearchDTO from(
            Page<SalesEntity> salesEntityPage,
            Map<Long, SalesDeliveryStatusEntity> deliveryStatusBySalesId
    ) {
        return ResSalesSearchDTO.builder()
                .salesPage(SalesPage.from(salesEntityPage, deliveryStatusBySalesId))
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
            return from(salesEntityPage, Map.of());
        }

        public static SalesPage from(
                Page<SalesEntity> salesEntityPage,
                Map<Long, SalesDeliveryStatusEntity> deliveryStatusBySalesId
        ) {
            return SalesPage.builder()
                    .content(SalesSummary.from(salesEntityPage.getContent(), deliveryStatusBySalesId))
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

            @Schema(example = "IN_TRANSIT")
            private String deliveryStatus;

            @Schema(example = "1234567890")
            private String trackingNumber;

            private LocalDateTime shippedAt;

            private LocalDateTime deliveredAt;

            private LocalDateTime cancelledAt;

            private LocalDateTime createdAt;

            public static List<SalesSummary> from(List<SalesEntity> salesEntityList) {
                return from(salesEntityList, Map.of());
            }

            public static List<SalesSummary> from(
                    List<SalesEntity> salesEntityList,
                    Map<Long, SalesDeliveryStatusEntity> deliveryStatusBySalesId
            ) {
                return salesEntityList.stream()
                        .map(sales -> SalesSummary.from(sales, deliveryStatusBySalesId.get(sales.getId())))
                        .toList();
            }

            public static SalesSummary from(SalesEntity sales) {
                return from(sales, null);
            }

            public static SalesSummary from(SalesEntity sales, SalesDeliveryStatusEntity deliveryStatus) {
                return SalesSummary.builder()
                        .salesId(sales.getId())
                        .status(sales.getStatus().name())
                        .itemCount(sales.getItems().size())
                        .totalQuantity(sales.getTotalQuantity())
                        .totalPrice(sales.getTotalPrice())
                        .deliveryStatus(deliveryStatus != null ? deliveryStatus.getStatus().name() : null)
                        .trackingNumber(deliveryStatus != null ? deliveryStatus.getTrackingNumber() : null)
                        .shippedAt(deliveryStatus != null ? deliveryStatus.getShippedAt() : null)
                        .deliveredAt(deliveryStatus != null ? deliveryStatus.getDeliveredAt() : null)
                        .cancelledAt(deliveryStatus != null ? deliveryStatus.getCancelledAt() : null)
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
