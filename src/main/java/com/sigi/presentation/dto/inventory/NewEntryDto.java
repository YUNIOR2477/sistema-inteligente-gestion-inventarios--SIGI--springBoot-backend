package com.sigi.presentation.dto.inventory;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(name = "NewInventoryEntryRequest", description = "Payload for creating new entry into the inventory")
public class NewEntryDto {
    private UUID inventoryId;
    private BigDecimal quantity;
    private UUID dispatcherId;
    private String motive;
}
