package com.sigi.presentation.dto.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(name = "NewInventoryRequest", description = "Payload for creating or updating an inventory record")
public class NewInventoryDto {

    @NotNull(message = "Product ID must not be null.")
    @Schema(description = "Unique identifier of the product associated with the inventory.",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID productId;

    @NotNull(message = "Warehouse ID must not be null.")
    @Schema(description = "Unique identifier of the warehouse where the inventory is stored.",
            example = "7fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID warehouseId;

    @Size(max = 128, message = "Location must not exceed 128 characters.")
    @Schema(description = "Specific location inside the warehouse.",
            example = "Shelf A3",
            maxLength = 128,
            nullable = true)
    private String location;

    @NotBlank(message = "Lot must not be empty.")
    @Size(max = 128, message = "Lot must not exceed 128 characters.")
    @Schema(description = "Lot identifier for the inventory batch.",
            example = "LOT-2025-001",
            maxLength = 128)
    private String lot;

    @PastOrPresent(message = "Production date cannot be in the future.")
    @Schema(description = "Production date of the product batch.",
            example = "2025-12-01",
            format = "date",
            nullable = true)
    private LocalDate productionDate;

    @Schema(description = "Expiration date of the product batch.",
            example = "2026-12-01",
            format = "date",
            nullable = true)
    private LocalDate expirationDate;

    @NotNull(message = "Available quantity must not be null.")
    @DecimalMin(value = "0.000", inclusive = true, message = "Available quantity must be greater than or equal to 0.")
    @Digits(integer = 12, fraction = 3, message = "Available quantity must have up to 12 digits and 3 decimals.")
    @Schema(description = "Available quantity in stock.",
            example = "150.000",
            type = "number",
            format = "decimal",
            minimum = "0.000")
    private BigDecimal availableQuantity = BigDecimal.ZERO;

    @DecimalMin(value = "0.000", inclusive = true, message = "Reserved amount must be greater than or equal to 0.")
    @Digits(integer = 12, fraction = 3, message = "Reserved amount must have up to 12 digits and 3 decimals.")
    @Schema(description = "Reserved quantity for orders.",
            example = "20.000",
            type = "number",
            format = "decimal",
            minimum = "0.000")
    private BigDecimal reservedQuantity = BigDecimal.ZERO;
}

