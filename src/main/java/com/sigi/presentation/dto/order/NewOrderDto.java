package com.sigi.presentation.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(name = "NewOrderRequest", description = "Payload for creating a new order")
public class NewOrderDto {

    @NotNull(message = "Client ID must not be null.")
    @Schema(description = "Unique identifier of the client placing the order.",
            example = "9fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID clientId;

    @NotNull(message = "Warehouse ID must not be null.")
    @Schema(description = "Unique identifier of the warehouse associated with the order.",
            example = "8fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID warehouseId;

    @NotNull(message = "Dispatcher ID must not be null.")
    @Schema(description = "Unique identifier of the dispatcher associated with the order.",
            example = "8fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID dispatcherId;
}

