package com.sigi.presentation.dto.dispatcher;

import com.sigi.presentation.dto.order.OrderDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(name = "DispatcherResponse", description = "Response containing details of a dispatcher record")
public class DispatcherDto {

    @Schema(description = "Unique identifier for the dispatcher (UUID).",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID id;

    @Schema(description = "Full name of the dispatcher.",
            example = "Juan Pérez")
    private String name;

    @Schema(description = "Primary contact person or reference for the dispatcher.",
            example = "Carlos Gómez")
    private String contact;

    @Schema(description = "Contact phone in E.164 format.",
            example = "+573001234567",
            pattern = "^\\+?[1-9]\\d{1,14}$")
    private String phone;

    @Schema(description = "Physical location or address of the dispatcher.",
            example = "Calle 123 #45-67, Bogotá, Colombia")
    private String location;

    private List<OrderDto> orders;

    @Schema(description = "Identification code for the dispatcher.",
            example = "DISP-10234")
    private String identification;

    @Schema(description = "Contact email address of the dispatcher.",
            example = "dispatcher@example.com",
            format = "email")
    private String email;

    @Schema(description = "Indicates whether the dispatcher is active.",
            example = "true")
    private Boolean active;

    @Schema(description = "Timestamp when the record was created (ISO 8601).",
            example = "2025-12-01T14:30:00+00:00",
            format = "date-time")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp of last update (ISO 8601).",
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

    @Schema(description = "Timestamp when the record was soft-deleted. Null when active.",
            example = "2026-01-05T12:00:00+00:00",
            format = "date-time",
            nullable = true)
    private LocalDateTime deletedAt;

    @Schema(description = "User who performed the soft-delete. Null when active.",
            example = "admin.user",
            nullable = true)
    private String deletedBy;
}


