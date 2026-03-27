package com.sigi.presentation.dto.client;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(name = "ClientResponse", description = "Response containing details of a client record")
public class ClientDto {

    @Schema(
            description = "Unique identifier for the client (UUID).",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
    )
    private UUID id;

    @Schema(
            description = "Full name of the client. Use the canonical display name (first + last).",
            example = "María Fernanda Gómez"
    )
    private String name;

    @Schema(
            description = "National or business identification number (format depends on country). Store only normalized value (no punctuation).",
            example = "1023456789",
            nullable = false
    )
    private String identification;

    @Schema(
            description = "Human readable location or address. Keep concise (street, city). For structured addresses use a dedicated object.",
            example = "Calle 10 # 5-20, Armenia, Quindío",
            maxLength = 255
    )
    private String location;

    @Schema(
            description = "Contact phone in E.164 format. Prefer storing normalized international format.",
            example = "+573001234567",
            pattern = "^\\+?[1-9]\\d{1,14}$"
    )
    private String phone;

    @Schema(
            description = "Contact email address. Use for notifications and login if applicable.",
            example = "cliente@example.com",
            format = "email"
    )
    private String email;

    @Schema(
            description = "Indicates whether the client is active. Soft-delete should set this to false and populate deletedAt/deletedBy.",
            example = "true"
    )
    private Boolean active;

    @Schema(
            description = "Timestamp when the record was created (ISO 8601, UTC offset). Use OffsetDateTime to preserve timezone info.",
            example = "2025-12-01T14:30:00+00:00",
            format = "date-time"
    )
    private LocalDateTime createdAt;

    @Schema(
            description = "Timestamp of last update (ISO 8601). Null if never updated.",
            example = "2025-12-10T09:15:00+00:00",
            format = "date-time",
            nullable = true
    )
    private LocalDateTime updatedAt;

    @Schema(
            description = "User or system identifier that created the record (username or id). Avoid exposing internal IDs if not necessary.",
            example = "system",
            maxLength = 100
    )
    private String createdBy;

    @Schema(
            description = "User or system identifier that last updated the record. Null if never updated.",
            example = "admin.user",
            nullable = true
    )
    private String updatedBy;

    @Schema(
            description = "Timestamp when the record was soft-deleted. Null when active.",
            example = "2026-01-05T12:00:00+00:00",
            format = "date-time",
            nullable = true
    )
    private LocalDateTime deletedAt;

    @Schema(
            description = "User who performed the soft-delete. Null when active.",
            example = "admin.user",
            nullable = true
    )
    private String deletedBy;
}