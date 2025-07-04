package com.project.chaechaeserver.presentation.controller.inventory;


import com.project.chaechaeserver.application.global.dto.ResDTO;
import com.project.chaechaeserver.application.response.inventory.ResCreateInventoryPostDTO;
import com.project.chaechaeserver.application.service.inventory.InventoryService;
import com.project.chaechaeserver.presentation.request.inventory.ReqCreateInventoryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping("/create")
    @Secured("ADMIN")
    public ResponseEntity<ResDTO<ResCreateInventoryPostDTO>> createInventory(@RequestBody
    ReqCreateInventoryDTO request) {

        return new ResponseEntity<>(ResDTO.<ResCreateInventoryPostDTO>builder()
            .code(HttpStatus.CREATED.value())
            .message("재고 생성 완료")
            .data(inventoryService.createInventory(request))
            .build(),
            HttpStatus.CREATED);

    }


}
