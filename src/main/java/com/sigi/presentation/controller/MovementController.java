package com.sigi.presentation.controller;

import com.sigi.presentation.dto.movement.MovementDto;
import com.sigi.presentation.dto.movement.NewMovementDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.services.service.movement.MovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = MovementDto.class)))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = INVALID_REQUEST,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = UNAUTHORIZED,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = INFO_NOT_FOUND,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = UNEXPECTED_ERROR,
        content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/GenericErrorResponse")))
@Tag(name = "Movement Controller", description = "Endpoints for managing inventory movements")
@Validated
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/movements")
public class MovementController {

    private final MovementService movementService;

    @Operation(summary = "Update Movement",
            description = "Updates an existing inventory movement by its ID.",
            operationId = "updateMovement",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_WAREHOUSE','ROLE_DISPATCHER')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MovementDto>> updateMovement(@PathVariable @NotNull UUID id,
                                                                   @RequestBody @NotNull NewMovementDto dto,
                                                                   HttpServletRequest request) {
        log.info("(updateMovement) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<MovementDto> response = movementService.updateMovement(id, dto);
        log.info("(updateMovement) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete Movement",
            description = "Soft deletes an inventory movement by its ID.",
            operationId = "deleteMovement",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMovement(@PathVariable @NotNull UUID id, HttpServletRequest request) {
        log.info("(deleteMovement) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = movementService.deleteMovement(id);
        log.info("(deleteMovement) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Restore Movement",
            description = "Restores a soft-deleted inventory movement by its ID.",
            operationId = "restoreMovement",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<Void>> restoreMovement(@PathVariable @NotNull UUID id, HttpServletRequest request) {
        log.info("(restoreMovement) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = movementService.restoreMovement(id);
        log.info("(restoreMovement) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get Movement by ID",
            description = "Retrieves details of a specific inventory movement by its ID.",
            operationId = "getMovementById",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MovementDto>> getMovementById(@PathVariable @NotNull UUID id, HttpServletRequest request) {
        log.info("(getMovementById) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<MovementDto> response = movementService.getMovementById(id);
        log.info("(getMovementById) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get deleted movement by ID",
            description = "Retrieves the deleted movement information identified by the provided ID.",
            operationId = "getDeletedMovementById",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR')")
    @GetMapping("/deleted/{id}")
    public ResponseEntity<ApiResponse<MovementDto>> getDeletedMovementById(@PathVariable @NotNull UUID id,
                                                                           HttpServletRequest request) {
        log.info("(getDeletedMovementById) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<MovementDto> response = movementService.getDeletedMovementById(id);
        log.info("(getDeletedMovementById) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List All Movements",
            description = "Retrieves a paginated list of all inventory movements.",
            operationId = "listAllActiveMovements",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @GetMapping("")
    public ResponseEntity<ApiResponse<PagedResponse<MovementDto>>> listAllActiveMovements(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                          HttpServletRequest request) {
        log.info("(listAllActiveMovements) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<MovementDto>> response = movementService.listAllActiveMovements(pagedRequestDto);
        log.info("(listAllActiveMovements) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "list Deleted Movements",
            description = "Retrieves a paginated list of soft-deleted inventory movements.",
            operationId = "listDeletedMovements",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR')")
    @GetMapping("/deleted")
    public ResponseEntity<ApiResponse<PagedResponse<MovementDto>>> listDeletedMovements(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                        HttpServletRequest request) {
        log.info("(listDeletedMovements) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<MovementDto>> response = movementService.listAllDeletedMovements(pagedRequestDto);
        log.info("(listDeletedMovements) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List Movements by Product",
            description = "Retrieves a paginated list of inventory movements filtered by product ID.",
            operationId = "listMovementsByProduct",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @GetMapping("/by-product")
    public ResponseEntity<ApiResponse<PagedResponse<MovementDto>>> listMovementsByProduct(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                          HttpServletRequest request) {
        log.info("(listMovementsByProduct) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<MovementDto>> response = movementService.listMovementsByProduct(pagedRequestDto);
        log.info("(listMovementsByProduct) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List Movements by Order",
            description = "Retrieves a paginated list of inventory movements filtered by order ID.",
            operationId = "listMovementsByOrder",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @GetMapping("/by-order")
    public ResponseEntity<ApiResponse<PagedResponse<MovementDto>>> listMovementsByOrder(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                        HttpServletRequest request) {
        log.info("(listMovementsByOrder) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<MovementDto>> response = movementService.listMovementsByOrder(pagedRequestDto);
        log.info("(listMovementsByOrder) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List Movements by Dispatcher",
            description = "Retrieves a paginated list of inventory movements filtered by dispatcher ID.",
            operationId = "listMovementsByDispatcher",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @GetMapping("/by-dispatcher")
    public ResponseEntity<ApiResponse<PagedResponse<MovementDto>>> listMovementsByDispatcher(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                             HttpServletRequest request) {
        log.info("(listMovementsByDispatcher) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<MovementDto>> response = movementService.listMovementsByDispatcher(pagedRequestDto);
        log.info("(listMovementsByDispatcher) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List Movements by Type",
            description = "Retrieves a paginated list of inventory movements filtered by movement type (IN/OUT).",
            operationId = "listMovementsByType",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @GetMapping("/by-type")
    public ResponseEntity<ApiResponse<PagedResponse<MovementDto>>> listMovementsByType(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                       HttpServletRequest request) {
        log.info("(listMovementsByType) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<MovementDto>> response = movementService.listMovementsByType(pagedRequestDto);
        log.info("(listMovementsByType) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List Movements Deleted by Type",
            description = "Retrieves a paginated list of soft-deleted inventory movements filtered by movement type (IN/OUT).",
            operationId = "listMovementsDeletedByType",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR')")
    @GetMapping("/deleted/by-type")
    public ResponseEntity<ApiResponse<PagedResponse<MovementDto>>> listMovementsDeletedByType(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                              HttpServletRequest request) {
        log.info("(listMovementsDeletedByType) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<MovementDto>> response = movementService.listDeletedMovementsByType(pagedRequestDto);
        log.info("(listMovementsDeletedByType) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }
}
