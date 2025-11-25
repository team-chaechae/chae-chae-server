package com.project.chaechaeserver.presentation.controller.order.docs;

import com.project.chaechaeserver.application.global.dto.ResDTO;
import com.project.chaechaeserver.application.response.order.ResCreateOrderCustomerPostDTO;
import com.project.chaechaeserver.presentation.request.order.ReqOrderCustomerPostCreateDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Order Customer", description = "Customer order management API")
public interface OrderCustomerControllerSwagger {

    @Operation(
        summary = "Create customer order",
        description = "Create a new customer order. Initial status is PENDING."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Order created successfully",
            content = @Content(schema = @Schema(implementation = ResCreateOrderCustomerPostDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request (validation failed)",
            content = @Content(schema = @Schema(implementation = ResDTO.class))
        )
    })
    ResponseEntity<ResDTO<ResCreateOrderCustomerPostDTO>> createOrder(
        @Valid @RequestBody ReqOrderCustomerPostCreateDTO request
    );

    @Operation(
        summary = "Get order details",
        description = "Retrieve order details by order ID."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Order retrieved successfully",
            content = @Content(schema = @Schema(implementation = ResCreateOrderCustomerPostDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Order not found",
            content = @Content(schema = @Schema(implementation = ResDTO.class))
        )
    })
    ResponseEntity<ResDTO<ResCreateOrderCustomerPostDTO>> getOrder(
        @Parameter(description = "Order ID", required = true)
        @PathVariable Long orderId
    );

    @Operation(
        summary = "Complete order",
        description = "Change order status to COMPLETED. Only available from PENDING status."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Order completed successfully"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid status transition",
            content = @Content(schema = @Schema(implementation = ResDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Order not found",
            content = @Content(schema = @Schema(implementation = ResDTO.class))
        )
    })
    ResponseEntity<ResDTO<Void>> completeOrder(
        @Parameter(description = "Order ID", required = true)
        @PathVariable Long orderId
    );
}
