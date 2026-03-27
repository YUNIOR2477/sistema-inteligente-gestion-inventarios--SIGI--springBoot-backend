package com.sigi.presentation.controller;

import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.presentation.dto.warehouse.NewWarehouseDto;
import com.sigi.presentation.dto.warehouse.WarehouseDto;
import com.sigi.services.service.warehouse.WarehouseService;
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

import static com.sigi.util.Constants.*;

import java.util.UUID;

@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = OPERATION_COMPLETED,
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = WarehouseDto.class)))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = INVALID_REQUEST,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = UNAUTHORIZED,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = INFO_NOT_FOUND,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = UNEXPECTED_ERROR,
        content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/GenericErrorResponse")))
@Tag(name = "Warehouse", description = "Endpoints for managing warehouses")
@Validated
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/warehouses")
public class WarehouseController {
    private final WarehouseService warehouseService;

    @Operation(summary = "Create a new warehouse",
            description = "Creates a new warehouse with the provided details",
            operationId = "createWarehouse",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping()
    public ResponseEntity<ApiResponse<WarehouseDto>> createWarehouse(@Valid @RequestBody NewWarehouseDto dto,
                                                                     HttpServletRequest request) {
        log.info("(createWarehouse) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<WarehouseDto> response = warehouseService.createWarehouse(dto);
        log.info("(createWarehouse) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update an existing warehouse",
            description = "Updates the details of an existing warehouse identified by its ID",
            operationId = "updateWarehouse",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WarehouseDto>> updateWarehouse(@PathVariable @NotNull UUID id,
                                                                     @Valid @RequestBody NewWarehouseDto dto,
                                                                     HttpServletRequest request) {
        log.info("(updateWarehouse) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<WarehouseDto> response = warehouseService.updateWarehouse(id, dto);
        log.info("(updateWarehouse) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete a warehouse",
            description = "Deletes an existing warehouse identified by its ID",
            operationId = "deleteWarehouse",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteWarehouse(@PathVariable @NotNull UUID id,
                                                             HttpServletRequest request) {
        log.info("(deleteWarehouse) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = warehouseService.deleteWarehouse(id);
        log.info("(deleteWarehouse) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Restore a deleted warehouse",
            description = "Restores a previously deleted warehouse identified by its ID",
            operationId = "restoreWarehouse",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<Void>> restoreWarehouse(@PathVariable @NotNull UUID id,
                                                              HttpServletRequest request) {
        log.info("(restoreWarehouse) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = warehouseService.restoreWarehouse(id);
        log.info("(restoreWarehouse) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get warehouse by ID",
            description = "Retrieves the details of a warehouse identified by its ID",
            operationId = "getWarehouseById",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WarehouseDto>> getWarehouseById(@PathVariable @NotNull UUID id,
                                                                      HttpServletRequest request) {
        log.info("(getWarehouseById) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<WarehouseDto> response = warehouseService.getWarehouseById(id);
        log.info("(getWarehouseById) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get deleted warehouse by ID",
            description = "Retrieves the deleted warehouse information identified by the provided ID.",
            operationId = "getDeletedWarehouseById",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR')")
    @GetMapping("/deleted/{id}")
    public ResponseEntity<ApiResponse<WarehouseDto>> getDeletedWarehouseById(@PathVariable @NotNull UUID id,
                                                                             HttpServletRequest request) {
        log.info("(getDeletedWarehouseById) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<WarehouseDto> response = warehouseService.getDeletedWarehouseById(id);
        log.info("(getDeletedWarehouseById) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List active warehouses",
            description = "Retrieves a paginated list of all active warehouses",
            operationId = "listActiveWarehouses",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @GetMapping()
    public ResponseEntity<ApiResponse<PagedResponse<WarehouseDto>>> listAllActiveWarehouse(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                           HttpServletRequest request) {
        log.info("(listAllActiveProducts) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<WarehouseDto>> response = warehouseService.listAllActiveWarehouse(pagedRequestDto);
        log.info("(listAllActiveProducts) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List deleted warehouses",
            description = "Retrieves a paginated list of all deleted warehouses",
            operationId = "listDeletedWarehouses",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR')")
    @GetMapping("/deleted")
    public ResponseEntity<ApiResponse<PagedResponse<WarehouseDto>>> listAllDeletedWarehouse(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                            HttpServletRequest request) {
        log.info("(listAllDeletedWarehouse) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<WarehouseDto>> response = warehouseService.listAllDeletedWarehouse(pagedRequestDto);
        log.info("(listAllDeletedWarehouse) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Search warehouses by capacity",
            description = "Retrieves a paginated list of warehouses with capacity greater than or equal to the specified value",
            operationId = "listWarehouseByCapacityGreaterOrEqual",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @GetMapping("/by-capacity")
    public ResponseEntity<ApiResponse<PagedResponse<WarehouseDto>>> listWarehouseByCapacityGreaterOrEqual(@RequestParam @NotNull Integer capacity,
                                                                                                          @ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                                          HttpServletRequest request) {
        log.info("(listWarehouseByCapacityGreaterOrEqual) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<WarehouseDto>> response = warehouseService.listWarehouseByCapacityGreaterOrEqual(capacity, pagedRequestDto);
        log.info("(listWarehouseByCapacityGreaterOrEqual) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Search warehouses by name",
            description = "Retrieves a paginated list of warehouses with names containing the specified string",
            operationId = "listWarehouseByName",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @GetMapping("/by-name")
    public ResponseEntity<ApiResponse<PagedResponse<WarehouseDto>>> listWarehouseByName(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                        HttpServletRequest request) {
        log.info("(listWarehouseByName) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<WarehouseDto>> response = warehouseService.listWarehouseByName(pagedRequestDto);
        log.info("(listWarehouseByName) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Search deleted warehouses by name",
            description = "Retrieves a paginated list of deleted warehouses with names containing the specified string",
            operationId = "listDeletedWarehouseByName",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR')")
    @GetMapping("/deleted/by-name")
    public ResponseEntity<ApiResponse<PagedResponse<WarehouseDto>>> listDeletedWarehouseByName(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                               HttpServletRequest request) {
        log.info("(listDeletedWarehouseByName) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<WarehouseDto>> response = warehouseService.listDeletedWarehouseByName(pagedRequestDto);
        log.info("(listDeletedWarehouseByName) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }
}


