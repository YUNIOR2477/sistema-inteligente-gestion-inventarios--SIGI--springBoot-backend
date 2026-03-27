package com.sigi.presentation.dto.inventory;

import com.sigi.presentation.dto.product.ProductDto;
import com.sigi.presentation.dto.warehouse.WarehouseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(name = "InventoryResponse", description = "Response containing details of an inventory record")
public class InventoryDto {

    @Schema(description = "Unique identifier for the inventory (UUID).",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID id;

    @Schema(description = "Product associated with this inventory record.",
            implementation = ProductDto.class)
    private ProductDto product;

    @Schema(description = "Warehouse where the inventory is stored.",
            implementation = WarehouseDto.class)
    private WarehouseDto warehouse;

    @Schema(description = "Specific location inside the warehouse.",
            example = "Shelf A3",
            maxLength = 128,
            nullable = true)
    private String location;

    @Schema(description = "Lot identifier for the inventory batch.",
            example = "LOT-2025-001",
            maxLength = 128)
    private String lot;

    @Schema(description = "Indicates whether the inventory record is active.",
            example = "true")
    private Boolean active;

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

    @Schema(description = "Available quantity in stock.",
            example = "150.000",
            type = "number",
            format = "decimal",
            minimum = "0.000")
    private BigDecimal availableQuantity;

    @Schema(description = "Reserved quantity for orders.",
            example = "20.000",
            type = "number",
            format = "decimal",
            minimum = "0.000")
    private BigDecimal reservedQuantity;

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
