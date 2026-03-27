package com.sigi.presentation.dto.order;

import com.sigi.presentation.dto.client.ClientDto;
import com.sigi.presentation.dto.dispatcher.DispatcherDto;
import com.sigi.presentation.dto.user.UserDto;
import com.sigi.presentation.dto.warehouse.WarehouseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(name = "OrderResponse", description = "Response containing details of an order")
public class OrderDto {

    @Schema(description = "Unique identifier for the order.",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID id;

    @Schema(description = "Client associated with the order.",
            implementation = ClientDto.class)
    private ClientDto client;

    private UserDto user;

    private WarehouseDto warehouse;

    private DispatcherDto dispatcher;

    @Schema(description = "Status of the order (DRAFT, CONFIRMED, DELIVERED, CANCELED).",
            example = "CONFIRMED")
    private String status;

    @Schema(description = "Total amount of the order.",
            example = "250000.00",
            type = "number",
            format = "decimal",
            minimum = "0.00")
    private BigDecimal total = BigDecimal.ZERO;

    private List<OrderLineDto> lines;

    @Schema(description = "Indicates whether the order is active.",
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

