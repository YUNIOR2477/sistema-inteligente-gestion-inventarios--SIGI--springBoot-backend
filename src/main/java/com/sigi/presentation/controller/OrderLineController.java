package com.sigi.presentation.controller;

import com.sigi.presentation.dto.order.NewOrderLineDto;
import com.sigi.presentation.dto.order.OrderLineDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.services.service.order.line.OrderLineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = OrderLineDto.class)))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = INVALID_REQUEST,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = UNAUTHORIZED,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = INFO_NOT_FOUND,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = UNEXPECTED_ERROR,
        content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/GenericErrorResponse")))
@Tag(name = "Online Orders", description = "Endpoints for managing online orders")
@Validated
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/order-lines")
public class OrderLineController {

    private final OrderLineService onlineOrderService;

    @Operation(summary = "Create Online Order Line",
            description = "Creates a new line item in an online order.",
            operationId = "createOrderLine",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SELLER')")
    @PostMapping()
    public ResponseEntity<ApiResponse<OrderLineDto>> createOrderLine(@RequestParam @NotNull UUID orderId,
                                                                     @Valid @RequestBody NewOrderLineDto dto,
                                                                     HttpServletRequest request) {
        log.info("(createOrderLine) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<OrderLineDto> response = onlineOrderService.createOrderLine(orderId, dto);
        log.info("(createOrderLine) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update Online Order Line",
            description = "Updates an existing line item in an online order.",
            operationId = "updateOrderLine",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SELLER')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderLineDto>> updateOrderLine(@PathVariable @NotNull UUID id,
                                                                     @Valid @RequestBody NewOrderLineDto dto,
                                                                     HttpServletRequest request) {
        log.info("(updateOrderLine) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<OrderLineDto> response = onlineOrderService.updateOrderLine(id, dto);
        log.info("(updateOrderLine) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete Online Order Line",
            description = "Deletes a line item from an online order.",
            operationId = "deleteOrderLine",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteOrderLine(@PathVariable @NotNull UUID id,
                                                             HttpServletRequest request) {
        log.info("(deleteOrderLine) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = onlineOrderService.deleteOrderLine(id);
        log.info("(deleteOrderLine) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Restore Deleted Online Order Line",
            description = "Restores a previously deleted line item in an online order.",
            operationId = "restoreOrderLine",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<Void>> restoreOrderLine(@PathVariable @NotNull UUID id,
                                                              HttpServletRequest request) {
        log.info("(restoreOrderLine) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = onlineOrderService.restoreOrderLine(id);
        log.info("(restoreOrderLine) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Online Order Line by ID",
            description = "Retrieves the details of a specific line item in an online order by its ID.",
            operationId = "getOrderLineById",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderLineDto>> getOrderLineById(@PathVariable @NotNull UUID id,
                                                                      HttpServletRequest request) {
        log.info("(getOrderLineById) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<OrderLineDto> response = onlineOrderService.getOrderLineById(id);
        log.info("(getOrderLineById) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get deleted order line by ID",
            description = "Retrieves the deleted order line information identified by the provided ID.",
            operationId = "getDeletedOrderLineById",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR')")
    @GetMapping("/deleted/{id}")
    public ResponseEntity<ApiResponse<OrderLineDto>> getDeletedOrderLineById(@PathVariable @NotNull UUID id,
                                                                             HttpServletRequest request) {
        log.info("(getDeletedOrderLineById) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<OrderLineDto> response = onlineOrderService.getDeletedOrderLineById(id);
        log.info("(getDeletedOrderLineById) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List Online Order Lines by Order",
            description = "Retrieves a paginated list of line items for a specific online order.",
            operationId = "listLinesByOrder",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @GetMapping("/by-order")
    public ResponseEntity<ApiResponse<PagedResponse<OrderLineDto>>> listLinesByOrder(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                     HttpServletRequest request) {
        log.info("(listLinesByOrder) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<OrderLineDto>> response = onlineOrderService.listLinesByOrder(pagedRequestDto);
        log.info("(listLinesByOrder) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List All Active Online Order Lines",
            description = "Retrieves a paginated list of all active line items across all online orders.",
            operationId = "listAllActiveLines",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<PagedResponse<OrderLineDto>>> listAllActiveLines(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                       HttpServletRequest request) {
        log.info("(listAllActiveLines) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<OrderLineDto>> response = onlineOrderService.listAllActiveLines(pagedRequestDto);
        log.info("(listAllActiveLines) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List All Deleted Online Order Lines",
            description = "Retrieves a paginated list of all deleted line items across all online orders.",
            operationId = "listAllDeletedLines",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR')")
    @GetMapping("/deleted")
    public ResponseEntity<ApiResponse<PagedResponse<OrderLineDto>>> listAllDeletedLines(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                        HttpServletRequest request) {
        log.info("(listAllDeletedLines) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<OrderLineDto>> response = onlineOrderService.listAllDeletedLines(pagedRequestDto);
        log.info("(listAllDeletedLines) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List Active Online Order Lines by Product Name",
            description = "Retrieves a paginated list of active line items filtered by product name across all online orders.",
            operationId = "listActiveLinesByProductName",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @GetMapping("/active/by-product-name")
    public ResponseEntity<ApiResponse<PagedResponse<OrderLineDto>>> listActiveLinesByProductName(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                                 HttpServletRequest request) {
        log.info("(listActiveLinesByProductName) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<OrderLineDto>> response = onlineOrderService.listActiveLinesByProductName(pagedRequestDto);
        log.info("( listActiveLinesByProductName) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List Deleted Online Order Lines by Product Name",
            description = "Retrieves a paginated list of deleted line items filtered by product name across all online orders.",
            operationId = "listDeletedLinesByProductName",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR')")
    @GetMapping("/deleted/by-product-name")
    public ResponseEntity<ApiResponse<PagedResponse<OrderLineDto>>> listDeletedLinesByProductName(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                                  HttpServletRequest request) {
        log.info("(listDeletedLinesByProductName) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<OrderLineDto>> response = onlineOrderService.listDeletedLinesByProductName(pagedRequestDto);
        log.info("( listDeletedLinesByProductName) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }


}
