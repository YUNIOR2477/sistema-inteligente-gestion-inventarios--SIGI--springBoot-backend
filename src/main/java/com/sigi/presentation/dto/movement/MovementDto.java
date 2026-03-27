package com.sigi.presentation.dto.movement;

import com.sigi.presentation.dto.dispatcher.DispatcherDto;
import com.sigi.presentation.dto.inventory.InventoryDto;
import com.sigi.presentation.dto.order.OrderDto;
import com.sigi.presentation.dto.product.ProductDto;
import com.sigi.presentation.dto.user.UserDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(name = "MovementResponse", description = "Response containing details of an inventory movement record")
public class MovementDto {

    @Schema(description = "Unique identifier for the movement (UUID).",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID id;

    @Schema(description = "Type of movement (ENTRY, EXIT, RESERVE, ADJUSTMENT).",
            example = "ENTRY")
    private String type;

    @Schema(description = "Inventory record associated with the movement.",
            implementation = InventoryDto.class)
    private InventoryDto inventory;

    @Schema(description = "Product involved in the movement.",
            implementation = ProductDto.class)
    private ProductDto product;

    @Schema(description = "Quantity of product moved.",
            example = "50.0000",
            type = "number",
            format = "decimal",
            minimum = "0.0001")
    private BigDecimal quantity;

    @Schema(description = "User who performed the movement.",
            implementation = UserDto.class)
    private UserDto user;

    @Schema(description = "Order associated with the movement, if applicable.",
            implementation = OrderDto.class,
            nullable = true)
    private OrderDto order;

    @Schema(description = "Dispatcher associated with the movement, if applicable.",
            implementation = DispatcherDto.class,
            nullable = true)
    private DispatcherDto dispatcher;

    @Schema(description = "Reason or motive for the movement.",
            example = "Stock adjustment due to damaged items",
            maxLength = 500,
            nullable = true)
    private String motive;

    @Schema(description = "Indicates whether the movement record is active.",
            example = "true")
    private Boolean active;

    @Schema(description = "Timestamp when the movement was created.",
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
