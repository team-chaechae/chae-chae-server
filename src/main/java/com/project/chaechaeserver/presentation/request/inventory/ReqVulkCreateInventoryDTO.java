package com.project.chaechaeserver.presentation.request.inventory;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReqVulkCreateInventoryDTO {

    private List<Inventory> inventory;


    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Inventory {

        private Long productId;
        private Integer quantity;

    }
}