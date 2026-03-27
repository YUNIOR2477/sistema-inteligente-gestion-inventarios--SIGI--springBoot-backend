package com.sigi.presentation.dto.product;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(name = "ProductResponse", description = "Response containing details of a product record")
public class ProductDto {

    @Schema(description = "Unique identifier for the product (UUID).",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID id;

    @Schema(description = "Stock Keeping Unit (SKU) code.",
            example = "SKU-12345")
    private String sku;

    @Schema(description = "Name of the product.",
            example = "Laptop Lenovo ThinkPad")
    private String name;

    @Schema(description = "Category to which the product belongs.",
            example = "Electronics")
    private String category;

    @Schema(description = "Unit of measurement for the product.",
            example = "piece")
    private String unit;

    @Schema(description = "Unit price of the product.",
            example = "2500.99",
            type = "number",
            format = "decimal")
    private BigDecimal price;

    @Schema(description = "Barcode of the product.",
            example = "7701234567890",
            nullable = true)
    private String barcode;

    @Schema(description = "Image URL of the product.",
            example = "https://example.com/images/product123.jpg",
            format = "uri",
            nullable = true)
    private String imageUrl;

    @Schema(description = "Indicates whether the product is active.",
            example = "true")
    private Boolean active;

    @Schema(description = "Timestamp when the record was created.",
            example = "2025-12-01T14:30:00+00:00",
            format = "date-time")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp of last update.",
            example = "2025-12-10T09:15:00+00:00",
            format = "date-time",
            nullable = true)
    private LocalDateTime updatedAt;

    @Schema(description = "User or system identifier that created the record.",
            example = "system")
    private String createdBy;

    @Schema(description = "User or system identifier that last updated the record.",
            example = "admin.user",
            nullable = true)
    private String updatedBy;

    @Schema(description = "Timestamp when the record was soft-deleted.",
            example = "2026-01-05T12:00:00+00:00",
            format = "date-time",
            nullable = true)
    private LocalDateTime deletedAt;

    @Schema(description = "User who performed the soft-delete.",
            example = "admin.user",
            nullable = true)
    private String deletedBy;
}

