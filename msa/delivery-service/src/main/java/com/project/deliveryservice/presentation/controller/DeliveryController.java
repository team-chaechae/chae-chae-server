package com.project.deliveryservice.presentation.controller;

import com.project.deliveryservice.application.global.dto.ResDeliveryDTO;
import com.project.deliveryservice.application.response.ResDTO;
import com.project.deliveryservice.application.service.DeliveryService;
import com.project.deliveryservice.presentation.request.ReqAssignTrackingDTO;
import com.project.deliveryservice.presentation.request.ReqCreateDeliveryDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/deliveries")
public class DeliveryController {

    private final DeliveryService deliveryService;

    @PostMapping
    public ResponseEntity<ResDTO<ResDeliveryDTO>> createDelivery(@Valid @RequestBody ReqCreateDeliveryDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ResDTO.created(deliveryService.createDelivery(request)));
    }

    @GetMapping("/{deliveryId}")
    public ResponseEntity<ResDTO<ResDeliveryDTO>> getDelivery(@PathVariable @Min(1) Long deliveryId) {
        return ResponseEntity.ok(ResDTO.success(deliveryService.getDelivery(deliveryId)));
    }

    @GetMapping("/sales/{salesId}")
    public ResponseEntity<ResDTO<ResDeliveryDTO>> getDeliveryBySalesId(@PathVariable @Min(1) Long salesId) {
        return ResponseEntity.ok(ResDTO.success(deliveryService.getDeliveryBySalesId(salesId)));
    }

    @PatchMapping("/{deliveryId}/tracking")
    public ResponseEntity<ResDTO<ResDeliveryDTO>> assignTrackingNumber(
            @PathVariable @Min(1) Long deliveryId,
            @Valid @RequestBody ReqAssignTrackingDTO request
    ) {
        return ResponseEntity.ok(ResDTO.success(deliveryService.assignTrackingNumber(deliveryId, request)));
    }

    @PatchMapping("/{deliveryId}/ship")
    public ResponseEntity<ResDTO<ResDeliveryDTO>> shipDelivery(@PathVariable @Min(1) Long deliveryId) {
        return ResponseEntity.ok(ResDTO.success(deliveryService.shipDelivery(deliveryId)));
    }

    @PatchMapping("/{deliveryId}/complete")
    public ResponseEntity<ResDTO<ResDeliveryDTO>> completeDelivery(@PathVariable @Min(1) Long deliveryId) {
        return ResponseEntity.ok(ResDTO.success(deliveryService.completeDelivery(deliveryId)));
    }

    @PatchMapping("/{deliveryId}/cancel")
    public ResponseEntity<ResDTO<ResDeliveryDTO>> cancelDelivery(@PathVariable @Min(1) Long deliveryId) {
        return ResponseEntity.ok(ResDTO.success(deliveryService.cancelDelivery(deliveryId)));
    }
}
