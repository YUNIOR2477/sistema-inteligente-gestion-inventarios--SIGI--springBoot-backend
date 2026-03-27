package com.sigi.presentation.dto.websocket.chatroom;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "NewChatRoomRequest", description = "Payload to create a new chat room")
public class NewChatRoomDto {
    @NotBlank(message = "Name must not be empty.")
    @Schema(description = "The name of the chat room.",
            example = "Project Alpha Team")
    private String name;

    @Schema(description = "List of initial participant user IDs.",
            example = "[\"d290f1ee-6c54-4b01-90e6-d701748f0851\", \"a123b456-c789-0d12-ef34-567890abcdef\"]")
    private List<UUID> participantIds;
}
