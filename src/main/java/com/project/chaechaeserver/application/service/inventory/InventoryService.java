package com.project.chaechaeserver.application.service.inventory;

import com.project.chaechaeserver.application.response.inventory.ResCreateInventoryPostDTO;
import com.project.chaechaeserver.application.response.inventory.ResUpdateInventoryPostDTO;
import com.project.chaechaeserver.presentation.request.inventory.ReqCreateInventoryDTO;

public interface InventoryService {

    ResCreateInventoryPostDTO addInventory(ReqCreateInventoryDTO dto);

    ResUpdateInventoryPostDTO decreaseInventory(ReqCreateInventoryDTO dto);
}
