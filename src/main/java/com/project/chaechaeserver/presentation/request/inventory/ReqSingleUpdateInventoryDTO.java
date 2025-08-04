package com.project.chaechaeserver.presentation.request.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReqSingleUpdateInventoryDTO {

    private Inventory inventory;


    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Inventory {

        private Long productId;
        private Integer quantity;

    }


}
