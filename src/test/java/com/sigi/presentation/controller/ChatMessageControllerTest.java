package com.sigi.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigi.configuration.security.jwt.JwtUtil;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.presentation.dto.websocket.chatmessage.ChatMessageDto;
import com.sigi.presentation.dto.websocket.chatmessage.NewChatMessageDto;
import com.sigi.services.service.websocket.chatmessage.ChatMessageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatMessageController.class)
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
class ChatMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private ChatMessageService chatMessageService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/v1/chat/by-room returns paged chat messages")
    void listMessagesByRoom_returnsPagedMessages() throws Exception {
        // Arrange
        ChatMessageDto dto = ChatMessageDto.builder()
                .id(UUID.randomUUID())
                .roomId(UUID.randomUUID())
                .senderEmail("user@example.com")
                .content("hello")
                .isRead(false)
                .sentAt(LocalDateTime.now())
                .build();

        Page<ChatMessageDto> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 10, Sort.by("sentAt").descending()), 1);
        ApiResponse<PagedResponse<ChatMessageDto>> apiResponse = ApiResponse.successPage("OK", page);

        when(chatMessageService.listMessagesByRoom(any(PagedRequestDto.class))).thenReturn(apiResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/chat/by-room")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortField", "sentAt")
                        .param("sortDirection", "DESC")
                        .param("searchValue", dto.getRoomId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content[0].content").value("hello"))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        verify(chatMessageService, times(1)).listMessagesByRoom(any(PagedRequestDto.class));
    }

    @Test
    @DisplayName("GET /api/v1/chat/by-sender returns paged messages by sender")
    void listMessagesBySender_returnsPagedMessages() throws Exception {
        // Arrange
        ChatMessageDto dto = ChatMessageDto.builder()
                .id(UUID.randomUUID())
                .roomId(UUID.randomUUID())
                .senderEmail("sender@example.com")
                .content("from sender")
                .isRead(true)
                .sentAt(LocalDateTime.now())
                .build();

        Page<ChatMessageDto> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 5), 1);
        ApiResponse<PagedResponse<ChatMessageDto>> apiResponse = ApiResponse.successPage("OK", page);

        when(chatMessageService.listMessagesBySender(any(PagedRequestDto.class))).thenReturn(apiResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/chat/by-sender")
                        .param("page", "0")
                        .param("size", "5")
                        .param("searchValue", "sender@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content[0].senderEmail").value("sender@example.com"));

        verify(chatMessageService, times(1)).listMessagesBySender(any(PagedRequestDto.class));
    }

    @Test
    @DisplayName("GET /api/v1/chat/unread-count/{roomId} returns unread count")
    void countUnreadMessages_returnsCount() throws Exception {
        // Arrange
        UUID roomId = UUID.randomUUID();
        ApiResponse<Long> apiResponse = ApiResponse.success("Unread notifications count", 7L);
        when(chatMessageService.countUnreadMessages(roomId)).thenReturn(apiResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/chat/unread-count/{roomId}", roomId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(7));

        verify(chatMessageService, times(1)).countUnreadMessages(roomId);
    }

    @Test
    @DisplayName("PUT /api/v1/chat/mark-as-read/{roomId} marks messages as read")
    void markMessagesAsRead_marksSuccessfully() throws Exception {
        // Arrange
        UUID roomId = UUID.randomUUID();
        ApiResponse<Void> apiResponse = ApiResponse.success("Notification marked as read", null);
        when(chatMessageService.markMessagesAsRead(roomId)).thenReturn(apiResponse);

        // Act & Assert
        mockMvc.perform(put("/api/v1/chat/mark-as-read/{roomId}", roomId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Notification marked as read"));

        verify(chatMessageService, times(1)).markMessagesAsRead(roomId);
    }

    @Test
    @DisplayName("MessageMapping method sendMessage delegates to service")
    void sendMessage_delegatesToService() {
        // Arrange
        ChatMessageController controller = new ChatMessageController(chatMessageService);
        NewChatMessageDto dto = NewChatMessageDto.builder()
                .roomId(UUID.randomUUID())
                .content("ws message")
                .senderId(UUID.randomUUID())
                .build();

        // Act
        controller.sendMessage(dto.getRoomId(), dto);

        // Assert
        ArgumentCaptor<NewChatMessageDto> captor = ArgumentCaptor.forClass(NewChatMessageDto.class);
        verify(chatMessageService, times(1)).sendMessageToRoom(captor.capture());
        NewChatMessageDto captured = captor.getValue();
        assertThat(captured.getContent()).isEqualTo("ws message");
        assertThat(captured.getRoomId()).isEqualTo(dto.getRoomId());
    }
}