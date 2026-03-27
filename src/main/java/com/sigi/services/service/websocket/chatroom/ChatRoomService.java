package com.sigi.services.service.websocket.chatroom;

import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.presentation.dto.websocket.chatroom.ChatRoomDto;
import com.sigi.presentation.dto.websocket.chatroom.NewChatRoomDto;

import java.util.List;
import java.util.UUID;

public interface ChatRoomService {
    ApiResponse<ChatRoomDto> createRoom(NewChatRoomDto dto);

    ApiResponse<Void> addParticipants(UUID roomId, List<UUID> userIds);

    ApiResponse<Void> removeParticipant(UUID roomId, UUID userId);

    ApiResponse<PagedResponse<ChatRoomDto>> listRoomsByUser(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<ChatRoomDto>> listRoomsByName(PagedRequestDto pagedRequestDto);

    ApiResponse<Boolean> userInRoom(String username, UUID roomId);

    ApiResponse<ChatRoomDto> getRoomById(UUID roomId);

    ApiResponse<Void> deactivateRoom(UUID roomId);
}
