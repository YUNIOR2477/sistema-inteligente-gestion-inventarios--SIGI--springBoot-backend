package com.sigi.presentation.dto.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(name = "NewWarehouseRequest", description = "Payload for creating or updating a warehouse")
public class NewWarehouseDto {

    @NotBlank(message = "Warehouse name must not be empty.")
    @Size(min = 3, max = 100, message = "Warehouse name must be between 3 and 100 characters.")
    @Schema(description = "Name of the warehouse. Must be unique and between 3 and 100 characters.",
            example = "Central Warehouse",
            minLength = 3,
            maxLength = 100)
    private String name;

    @NotBlank(message = "Location must not be empty.")
    @Size(min = 3, max = 150, message = "Location must be between 3 and 150 characters.")
    @Schema(description = "Physical location or address of the warehouse.",
            example = "Zona Industrial, Armenia, Quindío",
            minLength = 3,
            maxLength = 150)
    private String location;

    @NotNull(message = "Total capacity must not be null.")
    @Min(value = 1, message = "Total capacity must be at least 1.")
    @Max(value = 1000000, message = "Total capacity must not exceed 1,000,000 units.")
    @Schema(description = "Total storage capacity of the warehouse (units). Must be between 1 and 1,000,000.",
            example = "50000",
            minimum = "1",
            maximum = "1000000")
    private Integer totalCapacity;
}

