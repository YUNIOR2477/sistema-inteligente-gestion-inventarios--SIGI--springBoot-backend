package com.sigi.presentation.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
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
@Schema(name = "NewOnlineOrderRequest", description = "Payload for creating a new online order")
public class NewOrderLineDto {

    @NotNull(message = "Quantity must not be null.")
    @DecimalMin(value = "0.0001", inclusive = true, message = "Quantity must be greater than 0.")
    @Digits(integer = 12, fraction = 4, message = "Quantity must be a valid number with up to 4 decimal places.")
    @Schema(description = "Quantity of product ordered. Must be greater than 0.",
            example = "10.0000",
            type = "number",
            format = "decimal",
            minimum = "0.0001")
    private BigDecimal quantity;

    @NotNull(message = "Inventory ID must not be null.")
    @Schema(description = "Unique identifier of the inventory record associated with the order.",
            example = "7fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID inventoryId;
}

