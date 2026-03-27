package com.sigi.presentation.dto.websocket.chatroom;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "ChatRoomResponse", description = "Response containing details of a chat room")
public class ChatRoomDto {
    @Schema(
            description = "Unique identifier for the chat room (UUID).",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
    )
    private UUID id;
    @Schema(
            description = "Name of the chat room.",
            example = "General Discussion"
    )
    private String name;
    @Schema(
            description = "Indicates whether the chat room is active.",
            example = "true"
    )
    private Boolean active;
    @Schema(
            description = "List of participant user Emails in the chat room (UUIDs).",
            example = "[\"4b825dc6-8a5a-4f3d-9f3c-2c963f66afa6\", \"5c6f7e8d-9a0b-4c1d-8e2f-2c963f66afa6\"]"
    )
    private List<String> participantEmails;

    private List<UUID> participantIds;

    @Schema(
            description = "Timestamp when the chat room was created.",
            example = "2024-06-15T14:30:00"
    )
    private LocalDateTime createdAt;
    @Schema(
            description = "Timestamp when the chat room was last updated.",
            example = "2024-06-16T10:15:00"
    )
    private LocalDateTime updatedAt;

    private Integer unread = 0;
}
