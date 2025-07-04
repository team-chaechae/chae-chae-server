package com.project.chaechaeserver.application.service.inventory;

import com.project.chaechaeserver.application.response.inventory.ResCreateInventoryPostDTO;
import com.project.chaechaeserver.presentation.request.inventory.ReqCreateInventoryDTO;

public interface InventoryService {

    ResCreateInventoryPostDTO createInventory(ReqCreateInventoryDTO request);
}
