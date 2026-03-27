package com.sigi.presentation.dto.order;

import com.sigi.presentation.dto.inventory.InventoryDto;
import com.sigi.presentation.dto.product.ProductDto;
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
@Schema(name = "OnlineOrderResponse", description = "Response containing details of an online order")
public class OrderLineDto {

    @Schema(description = "Unique identifier for the online order.",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID id;

    @Schema(description = "Order associated with the online order.",
            implementation = OrderDto.class)
    private UUID orderId;

    @Schema(description = "Inventory associated with the online order.",
            implementation = InventoryDto.class)
    private InventoryDto inventory;

    @Schema(description = "Lot identifier for the product batch.",
            example = "LOT-2025-001")
    private String lot;

    @Schema(description = "Quantity of product ordered.",
            example = "10.0000",
            type = "number",
            format = "decimal")
    private BigDecimal quantity;

    @Schema(description = "Unit price of the product.",
            example = "25000.50",
            type = "number",
            format = "decimal")
    private BigDecimal unitPrice;

    @Schema(description = "Indicates whether the online order is active.",
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

