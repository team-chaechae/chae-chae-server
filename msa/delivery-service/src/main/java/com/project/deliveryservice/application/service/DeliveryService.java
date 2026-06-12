package com.project.deliveryservice.application.service;

import com.project.deliveryservice.application.global.dto.ResDeliveryDTO;
import com.project.deliveryservice.presentation.request.ReqAssignTrackingDTO;
import com.project.deliveryservice.presentation.request.ReqCreateDeliveryDTO;

public interface DeliveryService {

    ResDeliveryDTO createDelivery(ReqCreateDeliveryDTO request);

    ResDeliveryDTO createDeliveryIfAbsent(ReqCreateDeliveryDTO request);

    void createDeliveryFromEventIfAbsent(ReqCreateDeliveryDTO request);

    ResDeliveryDTO getDelivery(Long deliveryId);

    ResDeliveryDTO getDeliveryBySalesId(Long salesId);

    ResDeliveryDTO assignTrackingNumber(Long deliveryId, ReqAssignTrackingDTO request);

    ResDeliveryDTO shipDelivery(Long deliveryId);

    ResDeliveryDTO completeDelivery(Long deliveryId);

    ResDeliveryDTO cancelDelivery(Long deliveryId);

    void cancelDeliveryBySalesId(Long salesId, String orderId, String reason);
}
