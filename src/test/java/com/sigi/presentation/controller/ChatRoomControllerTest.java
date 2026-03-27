package com.sigi.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigi.configuration.security.jwt.JwtUtil;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.websocket.chatroom.ChatRoomDto;
import com.sigi.presentation.dto.websocket.chatroom.NewChatRoomDto;
import com.sigi.services.service.websocket.chatroom.ChatRoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(controllers = ChatRoomController.class)
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc(addFilters = false)
class ChatRoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private ChatRoomService chatRoomService;

    @Autowired
    private ObjectMapper objectMapper;

    private ChatRoomDto sampleRoom;
    private UUID roomId;

    @BeforeEach
    void setUp() {
        roomId = UUID.randomUUID();
        sampleRoom = ChatRoomDto.builder()
                .id(roomId)
                .name("Team Alpha")
                .active(true)
                .participantEmails(List.of("a@example.com", "b@example.com"))
                .participantIds(List.of(UUID.randomUUID(), UUID.randomUUID()))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .unread(0)
                .build();
    }

    @Test
    void createRoom_returnsCreatedRoom() throws Exception {
        NewChatRoomDto request = NewChatRoomDto.builder()
                .name("Team Alpha")
                .participantIds(List.of(UUID.randomUUID()))
                .build();

        when(chatRoomService.createRoom(any(NewChatRoomDto.class)))
                .thenReturn(ApiResponse.success("Room created", sampleRoom));

        mockMvc.perform(post("/api/v1/chat-rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("Team Alpha"))
                .andExpect(jsonPath("$.data.id").exists());

        ArgumentCaptor<NewChatRoomDto> captor = ArgumentCaptor.forClass(NewChatRoomDto.class);
        verify(chatRoomService, times(1)).createRoom(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Team Alpha");
    }

    @Test
    void addParticipants_delegatesAndReturnsOk() throws Exception {
        UUID user1 = UUID.randomUUID();
        List<UUID> users = List.of(user1);

        when(chatRoomService.addParticipants(eq(roomId), anyList()))
                .thenReturn(ApiResponse.success("Participants added", null));

        mockMvc.perform(put("/api/v1/chat-rooms/{roomId}/participants", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(users)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Participants added"));

        verify(chatRoomService, times(1)).addParticipants(eq(roomId), anyList());
    }

    @Test
    void removeParticipant_delegatesAndReturnsOk() throws Exception {
        UUID userId = UUID.randomUUID();

        when(chatRoomService.removeParticipant(roomId, userId))
                .thenReturn(ApiResponse.success("Participant removed", null));

        mockMvc.perform(put("/api/v1/chat-rooms/{roomId}/participants/{userId}", roomId, userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Participant removed"));

        verify(chatRoomService, times(1)).removeParticipant(roomId, userId);
    }

    @Test
    void listRoomsByUser_returnsPagedRooms() throws Exception {
        var page = new PageImpl<>(List.of(sampleRoom), PageRequest.of(0, 10), 1);
        when(chatRoomService.listRoomsByUser(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.successPage("OK", page));

        mockMvc.perform(get("/api/v1/chat-rooms/user")
                        .param("page", "0")
                        .param("size", "10")
                        .param("searchId", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content[0].name").value("Team Alpha"));

        verify(chatRoomService, times(1)).listRoomsByUser(any(PagedRequestDto.class));
    }

    @Test
    void listRoomsByName_returnsPagedRooms() throws Exception {
        var page = new PageImpl<>(List.of(sampleRoom), PageRequest.of(0, 5), 1);
        when(chatRoomService.listRoomsByName(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.successPage("OK", page));

        mockMvc.perform(get("/api/v1/chat-rooms/by-name")
                        .param("page", "0")
                        .param("size", "5")
                        .param("searchValue", "Team")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content[0].name").value("Team Alpha"));

        verify(chatRoomService, times(1)).listRoomsByName(any(PagedRequestDto.class));
    }

    @Test
    void getById_returnsRoom() throws Exception {
        when(chatRoomService.getRoomById(roomId))
                .thenReturn(ApiResponse.success("Found", sampleRoom));

        mockMvc.perform(get("/api/v1/chat-rooms/{roomId}", roomId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(roomId.toString()))
                .andExpect(jsonPath("$.data.name").value("Team Alpha"));

        verify(chatRoomService, times(1)).getRoomById(roomId);
    }

    @Test
    void deactivateRoom_delegatesAndReturnsOk() throws Exception {
        when(chatRoomService.deactivateRoom(roomId))
                .thenReturn(ApiResponse.success("Deactivated", null));

        mockMvc.perform(delete("/api/v1/chat-rooms/{roomId}", roomId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Deactivated"));

        verify(chatRoomService, times(1)).deactivateRoom(roomId);
    }

    @Test
    void createRoom_validationFails_whenNameBlank() throws Exception {
        NewChatRoomDto invalid = NewChatRoomDto.builder().name("").build();

        mockMvc.perform(post("/api/v1/chat-rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(chatRoomService);
    }
}