package com.sigi.presentation.controller;

import com.sigi.persistence.enums.OrderStatus;
import com.sigi.presentation.dto.order.NewOrderDto;
import com.sigi.presentation.dto.order.OrderDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.services.service.order.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.sigi.util.Constants.*;

@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = OPERATION_COMPLETED,
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderDto.class)))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = INVALID_REQUEST,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = UNAUTHORIZED,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = INFO_NOT_FOUND,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = UNEXPECTED_ERROR,
        content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/GenericErrorResponse")))
@Tag(name = "Order Controller", description = "APIs for managing orders")
@Validated
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService orderService;

    @Operation(
            summary = "Create a new order",
            description = "Create a new order with the provided details",
            operationId = "createOrder",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SELLER')")
    @PostMapping()
    public ResponseEntity<ApiResponse<OrderDto>> createOrder(@Valid @RequestBody NewOrderDto dto,
                                                             HttpServletRequest request) {
        log.info("(createOrder) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<OrderDto> response = orderService.createOrder(dto);
        log.info("(createOrder) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Update an existing order",
            description = "Update the details of an existing order by its ID",
            operationId = "updateOrder",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SELLER')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDto>> updateOrder(@PathVariable @NotNull UUID id,
                                                             @Valid @RequestBody NewOrderDto dto,
                                                             HttpServletRequest request) {
        log.info("(updateOrder) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<OrderDto> response = orderService.updateOrder(id, dto);
        log.info("(updateOrder) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete an order", description = "Delete an existing order by its ID", operationId = "deleteOrder",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable @NotNull UUID id,
                                                         HttpServletRequest request) {
        log.info("(deleteOrder) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = orderService.deleteOrder(id);
        log.info("(deleteOrder) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Restore a deleted order", description = "Restore a previously deleted order by its ID", operationId = "restoreOrder",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<Void>> restoreOrder(@PathVariable @NotNull UUID id,
                                                          HttpServletRequest request) {
        log.info("(restoreOrder) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = orderService.restoreOrder(id);
        log.info("(restoreOrder) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get order by ID",
            description = "Retrieve the details of an order by its unique ID",
            operationId = "getOrderById",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDto>> getOrderById(@PathVariable @NotNull UUID id, HttpServletRequest request) {
        log.info("(getOrderById) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<OrderDto> response = orderService.getOrderById(id);
        log.info("(getOrderById) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get deleted order by ID",
            description = "Retrieves the deleted order information identified by the provided ID.",
            operationId = "getDeletedOrderById",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR')")
    @GetMapping("/deleted/{id}")
    public ResponseEntity<ApiResponse<OrderDto>> getDeletedOrderById(@PathVariable @NotNull UUID id,
                                                                     HttpServletRequest request) {
        log.info("(getDeletedOrderById) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<OrderDto> response = orderService.getDeletedOrderById(id);
        log.info("(getDeletedOrderById) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "List all orders",
            description = "Retrieve a paginated list of all orders",
            operationId = "listAllActiveOrders",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @GetMapping()
    public ResponseEntity<ApiResponse<PagedResponse<OrderDto>>> listAllActiveOrders(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                    HttpServletRequest request) {
        log.info("(listAllActiveOrders) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<OrderDto>> response = orderService.listAllActiveOrders(pagedRequestDto);
        log.info("(listAllActiveOrders) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List deleted orders", description = "Retrieve a paginated list of all deleted orders", operationId = "listDeletedOrders",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR')")
    @GetMapping("/deleted")
    public ResponseEntity<ApiResponse<PagedResponse<OrderDto>>> listDeletedOrders(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                  HttpServletRequest request) {
        log.info("(listDeletedOrders) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<OrderDto>> response = orderService.listAllDeletedOrders(pagedRequestDto);
        log.info("(listDeletedOrders) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "List orders by client",
            description = "Retrieve a paginated list of orders for a specific client",
            operationId = "listOrdersByClient",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @GetMapping("/by-client")
    public ResponseEntity<ApiResponse<PagedResponse<OrderDto>>> listOrdersByClient(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                   HttpServletRequest request) {
        log.info("(listOrdersByClient) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<OrderDto>> response = orderService.listOrdersByClient(pagedRequestDto);
        log.info("(listOrdersByClient) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "List orders by client name",
            description = "Retrieve a paginated list of orders for a specific client by name",
            operationId = "listOrdersByClientName",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @GetMapping("/by-client-name")
    public ResponseEntity<ApiResponse<PagedResponse<OrderDto>>> listOrdersByClientName(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                       HttpServletRequest request) {
        log.info("(listOrdersByClientName) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<OrderDto>> response = orderService.listOrdersByClientName(pagedRequestDto);
        log.info("(listOrdersByClientName) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "List orders by user",
            description = "Retrieve a paginated list of orders created by a specific user",
            operationId = "listOrdersByUser",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @GetMapping("/by-user")
    public ResponseEntity<ApiResponse<PagedResponse<OrderDto>>> listOrdersByUser(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                 HttpServletRequest request) {
        log.info("(listOrdersByUser) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<OrderDto>> response = orderService.listOrdersByUser(pagedRequestDto);
        log.info("(listOrdersByUser) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "List orders by inventory",
            description = "Retrieve a list of orders created by a specific inventory",
            operationId = "listOrdersByInventory",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @GetMapping("/by-inventory")
    public ResponseEntity<ApiResponse<PagedResponse<OrderDto>>> listOrdersByInventory(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                      HttpServletRequest request) {
        log.info("(listOrdersByInventory) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<OrderDto>> response = orderService.listOrdersByInventory(pagedRequestDto);
        log.info("(listOrdersByInventory) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Cancel an order",
            description = "Cancel an existing order by its ID",
            operationId = "cancelOrder",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SELLER')")
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable @NotNull UUID id,
                                                         HttpServletRequest request) {
        log.info("(cancelOrder) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = orderService.cancelOrder(id);
        log.info("(cancelOrder) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Change order status",
            description = "Change the status of an existing order by its ID",
            operationId = "changeOrderStatus",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SELLER', 'ROLE_WAREHOUSE')")
    @PutMapping("/{id}/change-status")
    public ResponseEntity<ApiResponse<Void>> changeOrderStatus(@PathVariable @NotNull UUID id,
                                                               @RequestParam @NotBlank String status,
                                                               HttpServletRequest request) {
        log.info("(changeOrderStatus) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = orderService.changeOrderStatus(id, status);
        log.info("(changeOrderStatus) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }
}
