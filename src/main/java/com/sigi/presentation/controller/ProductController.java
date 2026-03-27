package com.sigi.presentation.controller;

import com.sigi.presentation.dto.product.NewProductDto;
import com.sigi.presentation.dto.product.ProductDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.services.service.product.ProductService;
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

import static com.sigi.util.Constants.*;

import java.math.BigDecimal;
import java.util.UUID;

@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = OPERATION_COMPLETED,
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductDto.class)))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = INVALID_REQUEST,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = UNAUTHORIZED,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = INFO_NOT_FOUND,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = UNEXPECTED_ERROR,
        content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/GenericErrorResponse")))
@Tag(name = "Product", description = "Product management endpoints")
@Validated
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private final ProductService productService;

    @Operation(summary = "Create a new product",
            description = "Creates a new product with the provided details.",
            operationId = "createProduct",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_WAREHOUSE')")
    @PostMapping()
    public ResponseEntity<ApiResponse<ProductDto>> createProduct(@Valid @RequestBody NewProductDto dto,
                                                                 HttpServletRequest request) {
        log.info("(createProduct) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<ProductDto> response = productService.createProduct(dto);
        log.info("(createProduct) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update an existing product",
            description = "Updates the details of an existing product identified by its ID.",
            operationId = "updateProduct",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_WAREHOUSE')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> updateProduct(@PathVariable @NotNull UUID id,
                                                                 @Valid @RequestBody NewProductDto dto,
                                                                 HttpServletRequest request) {
        log.info("(updateProduct) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<ProductDto> response = productService.updateProduct(id, dto);
        log.info("(updateProduct) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete a product",
            description = "Deletes a product identified by its ID.",
            operationId = "deleteProduct",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable @NotNull UUID id,
                                                           HttpServletRequest request) {
        log.info("(deleteProduct) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = productService.deleteProduct(id);
        log.info("(deleteProduct) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Restore a deleted product",
            description = "Restores a previously deleted product identified by its ID.",
            operationId = "restoreProduct",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/{id}/restore")
    public ResponseEntity<ApiResponse<Void>> restoreProduct(@PathVariable @NotNull UUID id,
                                                            HttpServletRequest request) {
        log.info("(restoreProduct) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = productService.restoreProduct(id);
        log.info("(restoreProduct) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get product by ID",
            description = "Retrieves a product by its unique ID.",
            operationId = "getProductById",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> getProductById(@PathVariable @NotNull UUID id,
                                                                  HttpServletRequest request) {
        log.info("(getProductById) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<ProductDto> response = productService.getProductById(id);
        log.info("(getProductById) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get deleted product by ID",
            description = "Retrieves the deleted product information identified by the provided ID.",
            operationId = "getDeletedProductById",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR')")
    @GetMapping("/deleted/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> getDeletedProductById(@PathVariable @NotNull UUID id,
                                                                         HttpServletRequest request) {
        log.info("(getDeletedProductById) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<ProductDto> response = productService.getDeletedProductById(id);
        log.info("(getDeletedProductById) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List all products",
            description = "Retrieves a paginated list of all products.",
            operationId = "listAllActiveProducts",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @GetMapping()
    public ResponseEntity<ApiResponse<PagedResponse<ProductDto>>> listAllActiveProducts(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                        HttpServletRequest request) {
        log.info("(listAllActiveProducts) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<ProductDto>> response = productService.listAllActiveProducts(pagedRequestDto);
        log.info("(listAllActiveProducts) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List all deleted products",
            description = "Retrieves a paginated list of all deleted products.",
            operationId = "listAllDeletedProducts",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR')")
    @GetMapping("/deleted")
    public ResponseEntity<ApiResponse<PagedResponse<ProductDto>>> listAllDeletedProducts(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                         HttpServletRequest request) {
        log.info("(listAllDeletedProducts) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<ProductDto>> response = productService.listAllDeletedProducts(pagedRequestDto);
        log.info("(listAllDeletedProducts) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get product by SKU",
            description = "Retrieves a product by its unique SKU.",
            operationId = "getProductBySku",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @GetMapping("/by-sku")
    public ResponseEntity<ApiResponse<ProductDto>> getProductBySku(@RequestParam @NotBlank String sku,
                                                                   HttpServletRequest request) {
        log.info("(getProductBySku) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<ProductDto> response = productService.getProductBySku(sku);
        log.info("(getProductBySku) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Search products by name",
            description = "Searches for products by their name with pagination.",
            operationId = "listProductsByName",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @GetMapping("/by-name")
    public ResponseEntity<ApiResponse<PagedResponse<ProductDto>>> listProductsByName(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                     HttpServletRequest request) {
        log.info("(listProductsByName) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<ProductDto>> response = productService.listProductsByName(pagedRequestDto);
        log.info("(listProductsByName) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Search deleted products by name",
            description = "Searches for deleted products by their name with pagination.",
            operationId = "listDeletedProductsByName",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR')")
    @GetMapping("/deleted/by-name")
    public ResponseEntity<ApiResponse<PagedResponse<ProductDto>>> listDeletedProductsByName(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                            HttpServletRequest request) {
        log.info("(listDeletedProductsByName) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<ProductDto>> response = productService.listDeletedProductsByName(pagedRequestDto);
        log.info("(listDeletedProductsByName) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Search products by category",
            description = "Searches for products by their category with pagination.",
            operationId = "listProductsByCategory",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @GetMapping("/by-category")
    public ResponseEntity<ApiResponse<PagedResponse<ProductDto>>> listProductsByCategory(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                         HttpServletRequest request) {
        log.info("(listProductsByCategory) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<ProductDto>> response = productService.listProductsByCategory( pagedRequestDto);
        log.info("(listProductsByCategory) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Search products by price range",
            description = "Searches for products within a specified price range with pagination.",
            operationId = "listProductsByPriceRange",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @GetMapping("/by-price-range")
    public ResponseEntity<ApiResponse<PagedResponse<ProductDto>>> listProductsByPriceRange(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                           @RequestParam @NotNull BigDecimal min,
                                                                                           @RequestParam @NotNull BigDecimal max,
                                                                                           HttpServletRequest request) {
        log.info("(listProductsByPriceRange) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<ProductDto>> response = productService.listProductsByPriceRange(min, max, pagedRequestDto);
        log.info("(listProductsByPriceRange) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }
}
