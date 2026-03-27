package com.sigi.presentation.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(name = "UserResponse", description = "Response containing details of a user record")
public class UserDto {

    @Schema(description = "Unique identifier for the user (UUID).",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID id;

    @Schema(description = "First name of the user.",
            example = "Carlos")
    private String name;

    @Schema(description = "Surname of the user.",
            example = "González Murillo")
    private String surname;

    @Schema(description = "Phone number of the user in international format.",
            example = "+573001234567",
            pattern = "^\\+?[1-9]\\d{1,14}$")
    private String phoneNumber;

    @Schema(description = "Email address of the user.",
            example = "user@example.com",
            format = "email")
    private String email;

    @Schema(description = "Role assigned to the user (e.g., ROLE_ADMIN, ROLE_CLIENT, ROLE_SELLER).",
            example = "ROLE_ADMIN")
    private String role;

    @Schema(description = "Indicates whether the user is active.",
            example = "true")
    private Boolean active;

    private boolean notificationsEnabled = true;

    private boolean chatNotificationsEnabled = true;

    private List<UUID> chatRoomsIds = new ArrayList<>();

    @Schema(description = "Timestamp of the user's last login.",
            example = "2026-01-05T12:00:00+00:00",
            format = "date-time",
            nullable = true)
    private LocalDateTime lastLogin;

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
            example = "2026-01-08T12:00:00+00:00",
            format = "date-time",
            nullable = true)
    private LocalDateTime deletedAt;

    @Schema(description = "User who performed the soft-delete.",
            example = "admin.user",
            nullable = true)
    private String deletedBy;
}

