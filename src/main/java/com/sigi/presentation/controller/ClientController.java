package com.sigi.presentation.controller;

import com.sigi.presentation.dto.client.ClientDto;
import com.sigi.presentation.dto.client.NewClientDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.services.service.client.ClientService;
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
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ClientDto.class)))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = INVALID_REQUEST,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = UNAUTHORIZED,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = INFO_NOT_FOUND,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = UNEXPECTED_ERROR,
        content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/GenericErrorResponse")))
@Tag(name = "Client", description = "Endpoints for managing clients")
@Validated
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {

    private final ClientService clientService;

    @Operation(summary = "Create a new client",
            description = "Creates a new client with the provided information.",
            operationId = "createClient",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SELLER')")
    @PostMapping()
    public ResponseEntity<ApiResponse<ClientDto>> createClient(@Valid @RequestBody NewClientDto dto,
                                                               HttpServletRequest request) {
        log.info("(createClient) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<ClientDto> response = clientService.createClient(dto);
        log.info("(createClient) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update an existing client",
            description = "Updates the information of an existing client identified by the provided ID.",
            operationId = "updateClient",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SELLER')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ClientDto>> updateClient(@PathVariable @NotNull UUID id,
                                                               @Valid @RequestBody NewClientDto dto,
                                                               HttpServletRequest request) {
        log.info("(updateClient) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<ClientDto> response = clientService.updateClient(id, dto);
        log.info("(updateClient) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete a client",
            description = "Deletes the client identified by the provided ID.",
            operationId = "deleteClient",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteClient(@PathVariable @NotNull UUID id,
                                                          HttpServletRequest request) {
        log.info("(deleteClient) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = clientService.deleteClient(id);
        log.info("(deleteClient) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Restore a deleted client",
            description = "Restores a previously deleted client identified by the provided ID.",
            operationId = "restoreClient",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<Void>> restoreClient(@PathVariable @NotNull UUID id,
                                                           HttpServletRequest request) {
        log.info("(restoreClient) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = clientService.restoreClient(id);
        log.info("(restoreClient) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get client by ID",
            description = "Retrieves the client information identified by the provided ID.",
            operationId = "getClientById",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SELLER', 'ROLE_AUDITOR')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClientDto>> getClientById(@PathVariable @NotNull UUID id,
                                                                HttpServletRequest request) {
        log.info("(getClientById) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<ClientDto> response = clientService.getClientById(id);
        log.info("(getClientById) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get deleted client by ID",
            description = "Retrieves the deleted client information identified by the provided ID.",
            operationId = "getDeletedClientById",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR')")
    @GetMapping("/deleted/{id}")
    public ResponseEntity<ApiResponse<ClientDto>> getDeletedClientById(@PathVariable @NotNull UUID id,
                                                                       HttpServletRequest request) {
        log.info("(getDeletedClientById) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<ClientDto> response = clientService.getDeletedClientById(id);
        log.info("(getDeletedClientById) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all clients",
            description = "Retrieves a paginated list of all clients.",
            operationId = "listAllActiveClients",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @GetMapping()
    public ResponseEntity<ApiResponse<PagedResponse<ClientDto>>> listAllActiveClients(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                      HttpServletRequest request) {
        log.info("(listAllActiveClients) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        log.info("Full request path: {}?{}", request.getRequestURL(), request.getQueryString());
        ApiResponse<PagedResponse<ClientDto>> response = clientService.listAllActiveClients(pagedRequestDto);
        log.info("(listAllActiveClients) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all deleted clients",
            description = "Retrieves a paginated list of all deleted clients.",
            operationId = "listAllDeletedClients",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR')")
    @GetMapping("/deleted")
    public ResponseEntity<ApiResponse<PagedResponse<ClientDto>>> listAllDeletedClients(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                       HttpServletRequest request) {
        log.info("(listAllDeletedClients) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<ClientDto>> response = clientService.listAllDeletedClients(pagedRequestDto);
        log.info("(listAllDeletedClients) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get client by identification",
            description = "Retrieves the client information identified by the provided identification number.",
            operationId = "getClientByIdentification",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @GetMapping("/by-identification")
    public ResponseEntity<ApiResponse<ClientDto>> getClientByIdentification(@RequestParam @NotBlank String identification,
                                                                            HttpServletRequest request) {
        log.info("(getClientByIdentification) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<ClientDto> response = clientService.getClientByIdentification(identification);
        log.info("(getClientByIdentification) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Search clients by name",
            description = "Searches for clients by their name and retrieves a paginated list of matching clients.",
            operationId = "listClientsByName",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @GetMapping("/by-name")
    public ResponseEntity<ApiResponse<PagedResponse<ClientDto>>> listClientsByName(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                   HttpServletRequest request) {
        log.info("(listClientsByName) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<ClientDto>> response = clientService.listClientsByName(pagedRequestDto);
        log.info("(listClientsByName) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Search deleted clients by name",
            description = "Searches for deleted clients by their name and retrieves a paginated list of matching clients.",
            operationId = "listDeletedClientsByName",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR')")
    @GetMapping("/deleted/by-name")
    public ResponseEntity<ApiResponse<PagedResponse<ClientDto>>> listDeletedClientsByName(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                          HttpServletRequest request) {
        log.info("(listDeletedClientsByName) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<ClientDto>> response = clientService.listClientDeletedByName(pagedRequestDto);
        log.info("(listDeletedClientsByName) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }
}
