package com.sigi.presentation.controller;

import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.presentation.dto.websocket.chatroom.ChatRoomDto;
import com.sigi.presentation.dto.websocket.chatroom.NewChatRoomDto;
import com.sigi.services.service.websocket.chatroom.ChatRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.sigi.util.Constants.*;

@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = OPERATION_COMPLETED,
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = ChatRoomDto.class)))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = INVALID_REQUEST,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = UNAUTHORIZED,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = INFO_NOT_FOUND,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = UNEXPECTED_ERROR,
        content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/GenericErrorResponse")))
@Tag(name = "Chat Room Management", description = "APIs for managing chat rooms")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/chat-rooms")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @Operation(
            summary = "Create new chat room",
            description = "Create a new chat room and return the chat room data",
            operationId = "createChatRoom"
    )
    @PostMapping
    public ResponseEntity<ApiResponse<ChatRoomDto>> createRoom(@Valid @RequestBody NewChatRoomDto dto,
                                                               HttpServletRequest request) {
        log.info("(createRoom) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<ChatRoomDto> response = chatRoomService.createRoom(dto);
        log.info("(createRoom) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Add participants to chat room",
            description = "Add participants to an existing chat room",
            operationId = "addParticipantsToChatRoom"
    )
    @PutMapping("/{roomId}/participants")
    public ResponseEntity<ApiResponse<Void>> addParticipants(@PathVariable @NotNull(message = ID_NOT_NULL) UUID roomId,
                                                             @RequestBody List<UUID> userIds,
                                                             HttpServletRequest request) {
        log.info("(addParticipants) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = chatRoomService.addParticipants(roomId, userIds);
        log.info("(addParticipants) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Remove participant from chat room",
            description = "Remove a participant from an existing chat room",
            operationId = "removeParticipantFromChatRoom"
    )
    @PutMapping("/{roomId}/participants/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeParticipant(@PathVariable @NotNull(message = ID_NOT_NULL) UUID roomId,
                                                               @PathVariable @NotNull(message = ID_NOT_NULL) UUID userId,
                                                               HttpServletRequest request) {
        log.info("(removeParticipant) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = chatRoomService.removeParticipant(roomId, userId);
        log.info("(removeParticipant) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "List chat rooms by user",
            description = "List all chat rooms in which a user participates",
            operationId = "listChatRoomsByUser"
    )
    @GetMapping("/user")
    public ResponseEntity<ApiResponse<PagedResponse<ChatRoomDto>>> listRoomsByUser(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                   HttpServletRequest request) {
        log.info("(listRoomsByUser) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<ChatRoomDto>> response = chatRoomService.listRoomsByUser(pagedRequestDto);
        log.info("(listRoomsByUser) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "List chat rooms by name",
            description = "List all chat rooms in which a user participates",
            operationId = "listRoomsByName"
    )
    @GetMapping("/by-name")
    public ResponseEntity<ApiResponse<PagedResponse<ChatRoomDto>>> listRoomsByName(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                   HttpServletRequest request) {
        log.info("(listRoomsByName) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<ChatRoomDto>> response = chatRoomService.listRoomsByName(pagedRequestDto);
        log.info("(listRoomsByName) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get chat room by ID",
            description = "Get an existing chat room by its ID",
            operationId = "getChatRoomById"
    )
    @GetMapping("/{roomId}")
    public ResponseEntity<ApiResponse<ChatRoomDto>> getById(@PathVariable @NotNull(message = ID_NOT_NULL) UUID roomId,
                                                            HttpServletRequest request) {
        log.info("(getChatRoomById) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<ChatRoomDto> response = chatRoomService.getRoomById(roomId);
        log.info("(getChatRoomById) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Deactivate chat room",
            description = "Deactivate (soft delete) an existing chat room",
            operationId = "deactivateChatRoom"
    )
    @DeleteMapping("/{roomId}")
    public ResponseEntity<ApiResponse<Void>> deactivateRoom(@PathVariable @NotNull(message = ID_NOT_NULL) UUID roomId,
                                                            HttpServletRequest request) {
        log.info("(deactivateRoom) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = chatRoomService.deactivateRoom(roomId);
        log.info("(deactivateRoom) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }
}

