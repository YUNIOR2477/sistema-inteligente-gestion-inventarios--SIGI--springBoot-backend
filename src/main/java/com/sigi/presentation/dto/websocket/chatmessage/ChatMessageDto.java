package com.sigi.presentation.dto.websocket.chatmessage;

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
@Schema(name = "ChatMessageResponse", description = "Response containing details of a chat message")
public class ChatMessageDto {
    @Schema(
            description = "Unique identifier for the chat message (UUID).",
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
    )
    private UUID id;
    @Schema(
            description = "Identifier of the chat room where the message was sent (UUID).",
            example = "4b825dc6-8a5a-4f3d-9f3c-2c963f66afa6"
    )
    private UUID roomId;
    @Schema(
            description = "Email of the user who sent the message.",
            example = "example@sigi.com"
    )
    private String senderEmail;
    @Schema(
            description = "Content of the chat message.",
            example = "Hello, how are you?"
    )
    private String content;
    @Schema(
            description = "Indicates whether the message has been read by the recipient.",
            example = "true"
    )
    private Boolean isRead;
    @Schema(
            description = "Timestamp when the message was sent.",
            example = "2024-06-15T14:30:00"
    )
    private LocalDateTime sentAt;
}
