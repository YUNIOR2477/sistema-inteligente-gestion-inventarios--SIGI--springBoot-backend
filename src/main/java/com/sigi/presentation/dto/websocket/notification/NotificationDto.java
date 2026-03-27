package com.sigi.presentation.dto.websocket.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "NotificationResponse", description = "Response containing details of a notification")
public class NotificationDto {
    @Schema(
            description = "Unique identifier for the notification (UUID).",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
    )
    private UUID id;
    @Schema(
            description = "Title of the notification.",
            example = "New Message Received"
    )
    private String title;
    @Schema(
            description = "Identifier of the related entity (UUID).",
            example = "4b825dc6-8a5a-4f3d-9f3c-2c963f66afa6"
    )

    private String message;
    @Schema(
            description = "Severity level of the notification.",
            example = "HIGH"
    )
    private Boolean isRead;
    @Schema(
            description = "Identifier of the user receiving the notification (UUID).",
            example = "5c6f7e8d-9a0b-4c1d-8e2f-2c963f66afa6"
    )
    private String user;

    @Schema(
            description = "Timestamp when the notification was created.",
            example = "2024-06-15T14:30:00"
    )
    private LocalDateTime createdAt;


}
