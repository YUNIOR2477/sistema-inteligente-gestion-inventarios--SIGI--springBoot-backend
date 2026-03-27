package com.sigi.services.service.websocket.chatmessage;

import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.presentation.dto.websocket.chatmessage.ChatMessageDto;
import com.sigi.presentation.dto.websocket.chatmessage.NewChatMessageDto;

import java.util.UUID;

public interface ChatMessageService {
    ApiResponse<ChatMessageDto> sendMessageToRoom(NewChatMessageDto dto);

    ApiResponse<PagedResponse<ChatMessageDto>> listMessagesByRoom(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<ChatMessageDto>> listMessagesBySender(PagedRequestDto pagedRequestDto);

    ApiResponse<Long> countUnreadMessages(UUID roomId);

    ApiResponse<Void> markMessagesAsRead(UUID roomId);
}
