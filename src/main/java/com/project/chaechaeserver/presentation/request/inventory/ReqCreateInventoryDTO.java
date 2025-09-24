package com.project.chaechaeserver.presentation.request.inventory;

import lombok.Getter;

@Getter
public class ReqCreateInventoryDTO {

    private Inventory inventory;


    @Getter
    public static class Inventory {

        private Long productId;
        private Integer quantity;

    }
}
