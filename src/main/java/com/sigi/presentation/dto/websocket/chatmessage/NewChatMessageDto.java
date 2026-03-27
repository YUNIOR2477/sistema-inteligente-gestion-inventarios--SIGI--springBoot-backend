package com.sigi.presentation.dto.websocket.chatmessage;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "ChatMessageRequest", description = "Payload to create a new chat message")
public class NewChatMessageDto {
    @NotBlank(message = "Room ID must not be empty.")
    @Schema(description = "Unique identifier of the chat room where the message is sent.",
            example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
    private UUID roomId;

    @NotBlank(message = "Content must not be empty.")
    @Schema(description = "The textual content of the chat message.",
            example = "Hello, how can I assist you today?")
    private String content;

    private UUID senderId;
}
