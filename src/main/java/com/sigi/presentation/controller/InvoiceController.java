package com.sigi.presentation.controller;

import com.sigi.persistence.enums.InvoiceStatus;
import com.sigi.presentation.dto.invoice.InvoiceDto;
import com.sigi.presentation.dto.invoice.NewInvoiceDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.services.service.invoice.InvoiceService;
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
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = InvoiceDto.class)))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = INVALID_REQUEST,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = UNAUTHORIZED,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = INFO_NOT_FOUND,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = UNEXPECTED_ERROR,
        content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/GenericErrorResponse")))
@Tag(name = "Invoice Management", description = "APIs for managing invoices")
@Slf4j
@RequiredArgsConstructor
@Validated
@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @Operation(summary = "Create a new invoice",
            description = "This endpoint allows you to create a new invoice by providing the necessary details in the request body.",
            operationId = "createInvoice",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SELLER')")
    @PostMapping()
    public ResponseEntity<ApiResponse<InvoiceDto>> createInvoice(@Valid @RequestBody NewInvoiceDto dto,
                                                                 HttpServletRequest request) {
        log.info("(createInvoice) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<InvoiceDto> response = invoiceService.createInvoice(dto);
        log.info("(createInvoice) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update an existing invoice",
            description = "This endpoint allows you to update an existing invoice by its ID and the new details provided in the request body.",
            operationId = "updateInvoice",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SELLER')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceDto>> updateInvoice(@PathVariable @NotNull UUID id,
                                                                 @Valid @RequestBody NewInvoiceDto dto,
                                                                 HttpServletRequest request) {
        log.info("(updateInvoice) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<InvoiceDto> response = invoiceService.updateInvoice(id, dto);
        log.info("(updateInvoice) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete an invoice",
            description = "This endpoint allows you to delete an existing invoice by its ID.",
            operationId = "deleteInvoice",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInvoice(@PathVariable @NotNull UUID id,
                                                           HttpServletRequest request) {
        log.info("(deleteInvoice) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = invoiceService.deleteInvoice(id);
        log.info("(deleteInvoice) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Restore a deleted invoice",
            description = "This endpoint allows you to restore a previously deleted invoice by its ID.",
            operationId = "restoreInvoice",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @PostMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<Void>> restoreInvoice(@PathVariable @NotNull UUID id,
                                                            HttpServletRequest request) {
        log.info("(restoreInvoice) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = invoiceService.restoreInvoice(id);
        log.info("(restoreInvoice) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get invoice by ID",
            description = "This endpoint retrieves an invoice by its unique ID.",
            operationId = "getInvoiceById",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceDto>> getInvoiceById(@PathVariable @NotNull UUID id,
                                                                  HttpServletRequest request) {
        log.info("(getInvoiceById) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<InvoiceDto> response = invoiceService.getInvoiceById(id);
        log.info("(getInvoiceById) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get deleted invoice by ID",
            description = "Retrieves the deleted invoice information identified by the provided ID.",
            operationId = "getDeletedInvoiceById",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR')")
    @GetMapping("/deleted/{id}")
    public ResponseEntity<ApiResponse<InvoiceDto>> getDeletedInvoiceById(@PathVariable @NotNull UUID id,
                                                                         HttpServletRequest request) {
        log.info("(getDeletedInvoiceById) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<InvoiceDto> response = invoiceService.getDeletedInvoiceById(id);
        log.info("(getDeletedInvoiceById) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List all invoices",
            description = "This endpoint retrieves a paginated list of all invoices.",
            operationId = "listAllActiveInvoices",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @GetMapping()
    public ResponseEntity<ApiResponse<PagedResponse<InvoiceDto>>> listAllActiveInvoices(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                        HttpServletRequest request) {
        log.info("(listAllActiveInvoices) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<InvoiceDto>> response = invoiceService.listAllActiveInvoices(pagedRequestDto);
        log.info("(listAllActiveInvoices) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List deleted invoices",
            description = "This endpoint retrieves a paginated list of all deleted invoices.",
            operationId = "listAllDeletedInvoices",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR')")
    @GetMapping("/deleted")
    public ResponseEntity<ApiResponse<PagedResponse<InvoiceDto>>> listAllDeletedInvoices(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                         HttpServletRequest request) {
        log.info("(listAllDeletedInvoices) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<InvoiceDto>> response = invoiceService.listAllDeletedInvoices(pagedRequestDto);
        log.info("(listAllDeletedInvoices) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get invoice by number",
            description = "This endpoint retrieves an invoice by its unique number.",
            operationId = "getInvoiceByNumber",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @GetMapping("/by-number")
    public ResponseEntity<ApiResponse<InvoiceDto>> getInvoiceByNumber(@RequestParam @NotNull String number,
                                                                      HttpServletRequest request) {
        log.info("(getInvoiceByNumber) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<InvoiceDto> response = invoiceService.getInvoiceByNumber(number);
        log.info("(getInvoiceByNumber) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get invoice by order id",
            description = "This endpoint retrieves an invoice by its order ID.",
            operationId = "getInvoiceByOrderId",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @GetMapping("/by-order")
    public ResponseEntity<ApiResponse<InvoiceDto>> getInvoiceByOrderId(@RequestParam @NotNull UUID orderId,
                                                                       HttpServletRequest request) {
        log.info("(getInvoiceByOrderId) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<InvoiceDto> response = invoiceService.getInvoiceByOrder(orderId);
        log.info("(getInvoiceByOrderId) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List invoices by client",
            description = "This endpoint retrieves a paginated list of invoices for a specific client.",
            operationId = "listInvoicesByClient",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @GetMapping("/by-client")
    public ResponseEntity<ApiResponse<PagedResponse<InvoiceDto>>> listInvoicesByClient(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                       HttpServletRequest request) {
        log.info("(listInvoicesByClient) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<InvoiceDto>> response = invoiceService.listInvoicesByClient(pagedRequestDto);
        log.info("(listInvoicesByClient) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List invoices by status",
            description = "This endpoint retrieves a paginated list of invoices filtered by their status.",
            operationId = "listInvoicesByStatus",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @GetMapping("/by-status")
    public ResponseEntity<ApiResponse<PagedResponse<InvoiceDto>>> listInvoicesByStatus(@RequestParam @NotNull InvoiceStatus status,
                                                                                       @ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                       HttpServletRequest request) {
        log.info("(listInvoicesByStatus) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<InvoiceDto>> response = invoiceService.listInvoicesByStatus(status, pagedRequestDto);
        log.info("(listInvoicesByStatus) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List deleted invoices by name",
            description = "This endpoint retrieves a paginated list of deleted invoices filtered by name.",
            operationId = "listDeletedInvoicesByNumber",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR')")
    @GetMapping("/deleted/by-number")
    public ResponseEntity<ApiResponse<PagedResponse<InvoiceDto>>> listDeletedInvoicesByNumber(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                              HttpServletRequest request) {
        log.info("(listDeletedInvoicesByNumber) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<InvoiceDto>> response = invoiceService.listDeletedInvoicesByNumber(pagedRequestDto);
        log.info("(listDeletedInvoicesByNumber) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Issue an invoice",
            description = "This endpoint allows you to issue an existing invoice by its ID.",
            operationId = "issueInvoice",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SELLER')")
    @PostMapping("/{id}/issue")
    public ResponseEntity<ApiResponse<InvoiceDto>> issueInvoice(@PathVariable @NotNull UUID id,
                                                                HttpServletRequest request) {
        log.info("(issueInvoice) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<InvoiceDto> response = invoiceService.issueInvoice(id);
        log.info("(issueInvoice) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Pay an invoice",
            description = "This endpoint allows you to pay an existing invoice by its ID.",
            operationId = "payInvoice",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SELLER')")
    @PostMapping("/{id}/pay")
    public ResponseEntity<ApiResponse<InvoiceDto>> payInvoice(@PathVariable @NotNull UUID id,
                                                              HttpServletRequest request) {
        log.info("(payInvoice) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<InvoiceDto> response = invoiceService.payInvoice(id);
        log.info("(payInvoice) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Cancel an invoice",
            description = "This endpoint allows you to cancel an existing invoice by its ID.",
            operationId = "cancelInvoice",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_SELLER')")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelInvoice(@PathVariable @NotNull UUID id,
                                                           HttpServletRequest request) {
        log.info("(cancelInvoice) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = invoiceService.cancelInvoice(id);
        log.info("(cancelInvoice) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }
}
