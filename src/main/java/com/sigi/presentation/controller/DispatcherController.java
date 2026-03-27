package com.sigi.presentation.controller;

import com.sigi.presentation.dto.dispatcher.DispatcherDto;
import com.sigi.presentation.dto.dispatcher.NewDispatcherDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.services.service.dispatcher.DispatcherService;
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
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = DispatcherDto.class)))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = INVALID_REQUEST,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = UNAUTHORIZED,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = INFO_NOT_FOUND,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = UNEXPECTED_ERROR,
        content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/GenericErrorResponse")))
@Tag(name = "Dispatcher", description = "Endpoints for managing dispatchers")
@Slf4j
@RequiredArgsConstructor
@Validated
@RestController
@RequestMapping("/api/v1/dispatchers")
public class DispatcherController {

    private final DispatcherService dispatcherService;

    @Operation(summary = "Create a new dispatcher",
            description = "Creates a new dispatcher with the provided details. Requires ADMIN role.",
            operationId = "createDispatcher",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_DISPATCHER')")
    @PostMapping()
    public ResponseEntity<ApiResponse<DispatcherDto>> createDispatcher(@Valid @RequestBody NewDispatcherDto dto,
                                                                       HttpServletRequest request) {
        log.info("(createDispatcher) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<DispatcherDto> response = dispatcherService.createDispatcher(dto);
        log.info("(createDispatcher) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update an existing dispatcher",
            description = "Updates the details of an existing dispatcher identified by ID. Requires ADMIN role.",
            operationId = "updateDispatcher",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_DISPATCHER')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DispatcherDto>> updateDispatcher(@PathVariable @NotNull UUID id,
                                                                       @Valid @RequestBody NewDispatcherDto dto,
                                                                       HttpServletRequest request) {
        log.info("(updateDispatcher) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<DispatcherDto> response = dispatcherService.updateDispatcher(id, dto);
        log.info("(updateDispatcher) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete a dispatcher",
            description = "Deletes the dispatcher identified by ID. Requires ADMIN role.",
            operationId = "deleteDispatcher",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDispatcher(@PathVariable @NotNull UUID id,
                                                              HttpServletRequest request) {
        log.info("(deleteDispatcher) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = dispatcherService.deleteDispatcher(id);
        log.info("(deleteDispatcher) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Restore a deleted dispatcher",
            description = "Restores a previously deleted dispatcher identified by ID. Requires ADMIN role.",
            operationId = "restoreDispatcher",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<Void>> restoreDispatcher(@PathVariable @NotNull UUID id,
                                                               HttpServletRequest request) {
        log.info("(restoreDispatcher) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = dispatcherService.restoreDispatcher(id);
        log.info("(restoreDispatcher) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get dispatcher by ID",
            description = "Retrieves the dispatcher details identified by ID. Requires ADMIN or DISPATCHER role.",
            operationId = "getDispatcherById",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DispatcherDto>> getDispatcherById(@PathVariable @NotNull UUID id,
                                                                        HttpServletRequest request) {
        log.info("(getDispatcherById) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<DispatcherDto> response = dispatcherService.getDispatcherById(id);
        log.info("(getDispatcherById) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get deleted dispatcher by ID",
            description = "Retrieves the deleted dispatcher information identified by the provided ID.",
            operationId = "getDeletedDispatcherById",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR')")
    @GetMapping("/deleted/{id}")
    public ResponseEntity<ApiResponse<DispatcherDto>> getDeletedDispatcherById(@PathVariable @NotNull UUID id,
                                                                               HttpServletRequest request) {
        log.info("(getDeletedDispatcherById) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<DispatcherDto> response = dispatcherService.getDeletedDispatcherById(id);
        log.info("(getDeletedDispatcherById) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all active dispatchers",
            description = "Retrieves a paginated list of all active dispatchers. Requires ADMIN or DISPATCHER role.",
            operationId = "getAllActiveDispatchers",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @GetMapping()
    public ResponseEntity<ApiResponse<PagedResponse<DispatcherDto>>> listAllActiveDispatchers(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                              HttpServletRequest request) {
        log.info("(getAllActiveDispatchers) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<DispatcherDto>> response = dispatcherService.listAllActiveDispatchers(pagedRequestDto);
        log.info("(getAllActiveDispatchers) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all deleted dispatchers",
            description = "Retrieves a paginated list of all deleted dispatchers. Requires ADMIN role.",
            operationId = "listAllDeletedDispatchers",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR')")
    @GetMapping("/deleted")
    public ResponseEntity<ApiResponse<PagedResponse<DispatcherDto>>> listAllDeletedDispatchers(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                               HttpServletRequest request) {
        log.info("(listAllDeletedDispatchers) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<DispatcherDto>> response = dispatcherService.listAllDeletedDispatchers(pagedRequestDto);
        log.info("(listAllDeletedDispatchers) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Search dispatchers by name",
            description = "Searches for dispatchers whose names match the provided query. Requires ADMIN or DISPATCHER role.",
            operationId = "listDispatcherByName",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @GetMapping("/by-name")
    public ResponseEntity<ApiResponse<PagedResponse<DispatcherDto>>> listDispatcherByName(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                          HttpServletRequest request) {
        log.info("(listDispatcherByName) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<DispatcherDto>> response = dispatcherService.listDispatcherByName(pagedRequestDto);
        log.info("(listDispatcherByName) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Search deleted dispatchers by name",
            description = "Searches for deleted dispatchers whose names match the provided query. Requires ADMIN role.",
            operationId = "listDeletedByName",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR')")
    @GetMapping("/deleted/by-name")
    public ResponseEntity<ApiResponse<PagedResponse<DispatcherDto>>> listDeletedByName(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                       HttpServletRequest request) {
        log.info("(listDeletedByName) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<DispatcherDto>> response = dispatcherService.listDeletedDispatcherByName( pagedRequestDto);
        log.info("(listDeletedByName) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }
}