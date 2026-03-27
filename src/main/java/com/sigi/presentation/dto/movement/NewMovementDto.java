package com.sigi.presentation.dto.movement;

import com.sigi.persistence.enums.MovementType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(name = "NewMovementRequest", description = "Payload for creating a new inventory movement")
public class NewMovementDto {

    @NotNull(message = "Type must not be null.")
    @Schema(description = "Type of movement. Allowed values: ENTRY, EXIT, RESERVE, ADJUSTMENT.",
            example = "ENTRY")
    private MovementType type;

    @NotNull(message = "Inventory ID must not be null.")
    @Schema(description = "Unique identifier of the inventory record associated with the movement.",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID inventoryId;

    @NotNull(message = "Product ID must not be null.")
    @Schema(description = "Unique identifier of the product involved in the movement.",
            example = "7fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID productId;

    @NotNull(message = "Quantity must not be null.")
    @DecimalMin(value = "0.0001", inclusive = true, message = "Quantity must be greater than 0.")
    @Digits(integer = 12, fraction = 4, message = "Quantity must be a valid number with up to 4 decimal places.")
    @Schema(description = "Quantity of product moved. Must be greater than 0.",
            example = "50.0000",
            type = "number",
            format = "decimal",
            minimum = "0.0001")
    private BigDecimal quantity;

    @Schema(description = "Unique identifier of the order associated with the movement.",
            example = "5fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID orderId = null;

    @Schema(description = "Unique identifier of the dispatcher associated with the movement.",
            example = "8fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID dispatcherId = null;

    @Size(max = 500, message = "Motive must not exceed 500 characters.")
    @Schema(description = "Reason or motive for the movement.",
            example = "Stock adjustment due to damaged items",
            maxLength = 500,
            nullable = true)
    private String motive;
}
