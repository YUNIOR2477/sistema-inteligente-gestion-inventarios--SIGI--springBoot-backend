package com.sigi.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.websocket.notification.NotificationDto;
import com.sigi.services.service.websocket.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID notificationId;
    private NotificationDto notificationDto;

    @Captor
    private ArgumentCaptor<PagedRequestDto> pagedRequestCaptor;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController).build();

        notificationId = UUID.randomUUID();
        notificationDto = NotificationDto.builder()
                .id(notificationId)
                .title("New message")
                .message("You have a new message")
                .isRead(false)
                .user("user@example.com")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getNotificationById_returnsNotification() throws Exception {
        when(notificationService.getNotificationById(notificationId))
                .thenReturn(ApiResponse.success("Found", notificationDto));

        mockMvc.perform(get("/api/v1/notifications/{notificationId}", notificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(notificationId.toString()))
                .andExpect(jsonPath("$.data.title").value("New message"));

        verify(notificationService, times(1)).getNotificationById(notificationId);
    }

    @Test
    void markAsRead_callsService_andReturnsOk() throws Exception {
        when(notificationService.markAsRead(notificationId)).thenReturn(ApiResponse.success("Marked", null));

        mockMvc.perform(put("/api/v1/notifications/mark-as-read/{notificationId}", notificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Marked"));

        verify(notificationService, times(1)).markAsRead(notificationId);
    }

    @Test
    void markAllAsRead_callsService_andReturnsOk() throws Exception {
        when(notificationService.markAllAsRead()).thenReturn(ApiResponse.success("All marked", null));

        mockMvc.perform(put("/api/v1/notifications/mark-all-as-read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("All marked"));

        verify(notificationService, times(1)).markAllAsRead();
    }

    @Test
    void listUnreadByUser_passesPagedRequest_andReturnsOk() throws Exception {
        when(notificationService.listUnreadByUser(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/notifications/unread")
                        .param("page", "0")
                        .param("size", "10")
                        .param("searchValue", "message"))
                .andExpect(status().isOk());

        verify(notificationService, times(1)).listUnreadByUser(pagedRequestCaptor.capture());
        PagedRequestDto captured = pagedRequestCaptor.getValue();
        assertThat(captured.getPage()).isEqualTo(0);
        assertThat(captured.getSize()).isEqualTo(10);
        assertThat(captured.getSearchValue()).isEqualTo("message");
    }

    @Test
    void listByUser_passesPagedRequest_andReturnsOk() throws Exception {
        when(notificationService.listByUser(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/notifications")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(notificationService, times(1)).listByUser(pagedRequestCaptor.capture());
        PagedRequestDto captured = pagedRequestCaptor.getValue();
        assertThat(captured.getPage()).isEqualTo(1);
        assertThat(captured.getSize()).isEqualTo(5);
    }

    @Test
    void countUnread_returnsNumber() throws Exception {
        when(notificationService.countUnread()).thenReturn(ApiResponse.success("OK", 7L));

        mockMvc.perform(get("/api/v1/notifications/count-unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(7));

        verify(notificationService, times(1)).countUnread();
    }

    @Test
    void listByTitle_passesPagedRequest_andReturnsOk() throws Exception {
        when(notificationService.listByTitle(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/notifications/by-tittle")
                        .param("searchValue", "welcome")
                        .param("page", "0")
                        .param("size", "3"))
                .andExpect(status().isOk());

        verify(notificationService, times(1)).listByTitle(pagedRequestCaptor.capture());
        PagedRequestDto captured = pagedRequestCaptor.getValue();
        assertThat(captured.getSearchValue()).isEqualTo("welcome");
        assertThat(captured.getSize()).isEqualTo(3);
    }
}