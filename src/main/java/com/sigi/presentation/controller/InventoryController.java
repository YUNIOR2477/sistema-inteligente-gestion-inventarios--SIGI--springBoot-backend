package com.sigi.presentation.controller;

import com.sigi.presentation.dto.inventory.*;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.services.service.inventory.InventoryService;
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
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = InventoryDto.class)))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = INVALID_REQUEST,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = UNAUTHORIZED,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = INFO_NOT_FOUND,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = UNEXPECTED_ERROR,
        content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/GenericErrorResponse")))
@Tag(name = "Inventory", description = "APIs for managing inventory records")
@Slf4j
@RequiredArgsConstructor
@Validated
@RestController
@RequestMapping("/api/v1/inventories")
public class InventoryController {
    private final InventoryService inventoryService;

    @Operation(summary = "Create a new inventory record",
            description = "Creates a new inventory record with the provided details. Requires ADMIN or WAREHOUSE role.",
            operationId = "createInventory",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_WAREHOUSE')")
    @PostMapping()
    public ResponseEntity<ApiResponse<InventoryDto>> createInventory(@Valid @RequestBody NewInventoryDto dto,
                                                                     HttpServletRequest request) {
        log.info("(createInventory) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<InventoryDto> response = inventoryService.createInventory(dto);
        log.info("(createInventory) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);

    }

    @Operation(summary = "Update an existing inventory record",
            description = "Updates an existing inventory record with the provided details. Requires ADMIN or WAREHOUSE role.",
            operationId = "updateInventory",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_WAREHOUSE')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryDto>> updateInventory(@PathVariable @NotNull UUID id,
                                                                     @Valid @RequestBody NewInventoryDto dto,
                                                                     HttpServletRequest request) {
        log.info("(updateInventory) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<InventoryDto> response = inventoryService.updateInventory(id, dto);
        log.info("(updateInventory) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);

    }

    @Operation(summary = "Delete an inventory record",
            description = "Deletes an inventory record by its ID. Requires ADMIN or WAREHOUSE role.",
            operationId = "deleteInventory",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInventory(@PathVariable @NotNull UUID id,
                                                             HttpServletRequest request) {
        log.info("(deleteInventory) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = inventoryService.deleteInventory(id);
        log.info("(deleteInventory) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);

    }

    @Operation(summary = "Restore deleted inventory",
            description = "Restores a previously deleted inventory record by its ID. Requires ADMIN or WAREHOUSE role.",
            operationId = "restoreInventory",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<Void>> restoreInventory(@PathVariable @NotNull UUID id,
                                                              HttpServletRequest request) {
        log.info("(restoreInventory) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = inventoryService.restoreInventory(id);
        log.info("(restoreInventory) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get inventory by ID",
            description = "Retrieves an inventory record by its ID.",
            operationId = "getInventoryById",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryDto>> getInventoryById(@PathVariable @NotNull UUID id,
                                                                      HttpServletRequest request) {
        log.info("(getInventoryById) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<InventoryDto> response = inventoryService.getInventoryById(id);
        log.info("(getInventoryById) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get deleted inventory by ID",
            description = "Retrieves the deleted inventory information identified by the provided ID.",
            operationId = "getDeletedInventoryById",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR')")
    @GetMapping("/deleted/{id}")
    public ResponseEntity<ApiResponse<InventoryDto>> getDeletedInventoryById(@PathVariable @NotNull UUID id,
                                                                             HttpServletRequest request) {
        log.info("(getDeletedInventoryById) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<InventoryDto> response = inventoryService.getDeletedInventoryById(id);
        log.info("(getDeletedInventoryById) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List all inventories",
            description = "Lists all inventory records with pagination support.",
            operationId = "listAllInventories",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @GetMapping()
    public ResponseEntity<ApiResponse<PagedResponse<InventoryDto>>> listAllInventories(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                       HttpServletRequest request) {
        log.info("(listAllInventories) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<InventoryDto>> response = inventoryService.listAllInventories(pagedRequestDto);
        log.info("(listAllInventories) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List deleted inventories",
            description = "Lists inventory records that have been marked as deleted, with pagination support.",
            operationId = "listDeletedInventories",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/deleted")
    public ResponseEntity<ApiResponse<PagedResponse<InventoryDto>>> listDeletedInventories(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                           HttpServletRequest request) {
        log.info("(listDeletedInventories) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<InventoryDto>> response = inventoryService.listAllDeletedInventories(pagedRequestDto);
        log.info("(listDeletedInventories) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }


    @Operation(summary = "List inventories by warehouse",
            description = "Lists inventory records for a specific warehouse with pagination support.",
            operationId = "listInventoriesByWarehouse",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @GetMapping("/by-warehouse")
    public ResponseEntity<ApiResponse<PagedResponse<InventoryDto>>> listInventoriesByWarehouse(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                               HttpServletRequest request) {
        log.info("(listInventoriesByWarehouse) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<InventoryDto>> response = inventoryService.listInventoriesByWarehouse(pagedRequestDto);
        log.info("(listInventoriesByWarehouse) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List inventories by product",
            description = "Lists inventory records for a specific product with pagination support.",
            operationId = "listInventoriesByProduct",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @GetMapping("/by-product")
    public ResponseEntity<ApiResponse<PagedResponse<InventoryDto>>> listInventoriesByProduct(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                             HttpServletRequest request) {
        log.info("(listInventoriesByProduct) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<InventoryDto>> response = inventoryService.listInventoriesByProduct(pagedRequestDto);
        log.info("(listInventoriesByProduct) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Search low stock inventories",
            description = "Searches for inventory records with available quantity below a specified threshold, with pagination support.",
            operationId = "listInventoriesByLowStock",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<PagedResponse<InventoryDto>>> listInventoriesByLowStock(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                              HttpServletRequest request) {
        log.info("(listInventoriesByLowStock) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<InventoryDto>> response = inventoryService.listInventoriesByLowStock(pagedRequestDto);
        log.info("(listInventoriesByLowStock) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List available inventories by warehouse",
            description = "Lists inventory records with available stock for a specific warehouse, with pagination support.",
            operationId = "listAvailableInventoriesByWarehouse",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @GetMapping("/by-available-warehouse")
    public ResponseEntity<ApiResponse<PagedResponse<InventoryDto>>> listAvailableInventoriesByWarehouseId(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                                          HttpServletRequest request) {
        log.info("(listAvailableInventoriesByWarehouse) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<InventoryDto>> response = inventoryService.listAvailableInventoriesByWarehouse(pagedRequestDto);
        log.info("(listAvailableInventoriesByWarehouse) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Search inventories by product name",
            description = "Searches for inventory records that have been marked as active based on product name, with pagination support.",
            operationId = "listInventoriesByProductName",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @GetMapping("/by-product-name")
    public ResponseEntity<ApiResponse<PagedResponse<InventoryDto>>> listInventoriesByProductName(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                                 HttpServletRequest request) {
        log.info("(listInventoriesByProductName) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<InventoryDto>> response = inventoryService.listInventoriesByProductName(pagedRequestDto);
        log.info("(listInventoriesByProductName) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Search inventories by product sku",
            description = "Searches for inventory records that have been marked as active based on product sku, with pagination support.",
            operationId = "listInventoriesByProductSku",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @GetMapping("/by-product-sku")
    public ResponseEntity<ApiResponse<PagedResponse<InventoryDto>>> listInventoriesByProductSku(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                                 HttpServletRequest request) {
        log.info("(listInventoriesByProductSku) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<InventoryDto>> response = inventoryService.listInventoriesByProductSku(pagedRequestDto);
        log.info("(listInventoriesByProductSku) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Search deleted inventories by product name",
            description = "Searches for inventory records that have been marked as deleted based on product name, with pagination support.",
            operationId = "listDeletedInventoriesByProductName",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/deleted/by-product-name")
    public ResponseEntity<ApiResponse<PagedResponse<InventoryDto>>> listDeletedInventoriesByProductName(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                                        HttpServletRequest request) {
        log.info("(listDeletedInventoriesByProductName) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<InventoryDto>> response = inventoryService.listDeletedInventoriesByProductName(pagedRequestDto);
        log.info("(listDeletedInventoriesByProductName) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Register inventory entry",
            description = "Registers an entry of stock into the inventory.",
            operationId = "registerInventoryEntry",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_WAREHOUSE', 'ROLE_DISPATCHER')")
    @PostMapping("/register-entry")
    public ResponseEntity<ApiResponse<Void>> registerInventoryEntry(@RequestBody NewEntryDto newEntry,
                                                                    HttpServletRequest request) {
        log.info("(registerInventoryEntry) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = inventoryService.registerInventoryEntry(newEntry);
        log.info("(registerInventoryEntry) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Register inventory transfer ",
            description = "Register a stock output from the inventory due to transfers between warehouses.",
            operationId = "registerInventoryTransfer",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_WAREHOUSE', 'ROLE_DISPATCHER')")
    @PostMapping("/register-transfer")
    public ResponseEntity<ApiResponse<Void>> registerInventoryTransfer(@RequestBody ExitTransferDto exitTransferDto,
                                                                       HttpServletRequest request) {
        log.info("(registerInventoryTransfer) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = inventoryService.registerInventoryTransfer(exitTransferDto);
        log.info("(registerInventoryTransfer) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Register inventory exit for disposal ",
            description = "Register a stock output from the inventory for disposal.",
            operationId = "registerInventoryExitForDisposal",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_WAREHOUSE', 'ROLE_DISPATCHER')")
    @PostMapping("/register-disposal")
    public ResponseEntity<ApiResponse<Void>> registerInventoryExitForDisposal(@RequestBody ExitDisposalDto exitDisposalDto,
                                                                              HttpServletRequest request) {
        log.info("(registerInventoryExitForDisposal) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = inventoryService.registerInventoryExitForDisposal(exitDisposalDto);
        log.info("(registerInventoryExitForDisposal) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

}
