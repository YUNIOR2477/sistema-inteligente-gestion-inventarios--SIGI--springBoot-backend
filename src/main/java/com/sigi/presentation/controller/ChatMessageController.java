package com.sigi.presentation.controller;

import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.presentation.dto.websocket.chatmessage.ChatMessageDto;
import com.sigi.presentation.dto.websocket.chatmessage.NewChatMessageDto;
import com.sigi.services.service.websocket.chatmessage.ChatMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.sigi.util.Constants.*;

@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = OPERATION_COMPLETED,
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChatMessageDto.class)))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = INVALID_REQUEST,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = UNAUTHORIZED,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = INFO_NOT_FOUND,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = UNEXPECTED_ERROR,
        content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/GenericErrorResponse")))
@Tag(name = "chat-message", description = "Operations related to chat messages")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/chat")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    @MessageMapping("/chat.send/{roomId}")
    public void sendMessage(
            @DestinationVariable UUID roomId,
            NewChatMessageDto msg
    ) {
        chatMessageService.sendMessageToRoom(msg);
    }

    @Operation(
            summary = "List messages by room",
            description = "Retrieve a paginated list of chat messages for a specific chat room",
            operationId = "listMessagesByRoom"
    )
    @GetMapping("/by-room")
    public ResponseEntity<ApiResponse<PagedResponse<ChatMessageDto>>> listMessagesByRoom(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                         HttpServletRequest request) {
        log.info("(listMessagesByRoom) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<ChatMessageDto>> response = chatMessageService.listMessagesByRoom(pagedRequestDto);
        log.info("(listMessagesByRoom) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "List messages by sender",
            description = "Retrieve a paginated list of chat messages sent by a specific user",
            operationId = "listMessagesBySender"
    )
    @GetMapping("/by-sender")
    public ResponseEntity<ApiResponse<PagedResponse<ChatMessageDto>>> listMessagesBySender(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                           HttpServletRequest request) {
        log.info("(listMessagesBySender) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<ChatMessageDto>> response = chatMessageService.listMessagesBySender(pagedRequestDto);
        log.info("(listMessagesBySender) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Count unread messages",
            description = "Count the number of unread messages in a specific chat room",
            operationId = "countUnreadMessages"
    )
    @GetMapping("/unread-count/{roomId}")
    public ResponseEntity<ApiResponse<Long>> countUnreadMessages(@PathVariable @NotNull(message = ID_NOT_NULL) UUID roomId,
                                                                 HttpServletRequest request) {
        log.info("(countUnreadMessages) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Long> response = chatMessageService.countUnreadMessages(roomId);
        log.info("(countUnreadMessages) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Mark messages as read",
            description = "Mark all messages in a specific chat room as read",
            operationId = "markMessagesAsRead"
    )
    @PutMapping("/mark-as-read/{roomId}")
    public ResponseEntity<ApiResponse<Void>> markMessagesAsRead(@PathVariable @NotNull(message = ID_NOT_NULL) UUID roomId,
                                                                HttpServletRequest request) {
        log.info("(markMessagesAsRead) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = chatMessageService.markMessagesAsRead(roomId);
        log.info("(markMessagesAsRead) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }
}
