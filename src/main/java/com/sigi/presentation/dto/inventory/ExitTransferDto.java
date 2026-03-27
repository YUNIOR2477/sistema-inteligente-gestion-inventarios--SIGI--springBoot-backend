package com.sigi.presentation.dto.inventory;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class ExitTransferDto {
    @NotNull(message = "Origin inventoryId is required")
    private UUID originInventoryId;

    @NotNull(message = "Destination inventoryId is required")
    private UUID destinationInventoryId;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    @Size(max = 256, message = "Motive must not exceed 256 characters")
    private String motive;

}
