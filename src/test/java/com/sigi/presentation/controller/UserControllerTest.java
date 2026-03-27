package com.sigi.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigi.presentation.dto.auth.NewUserDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.user.MetricsDto;
import com.sigi.presentation.dto.user.UpdateUserProfileDto;
import com.sigi.presentation.dto.user.UserDto;
import com.sigi.persistence.enums.RoleList;
import com.sigi.services.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for UserController using MockMvc (standalone) and Mockito.
 * Focus: routing, request binding, response shape and service interaction.
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID userId;
    private UserDto userDto;
    private NewUserDto newUserDto;
    private UpdateUserProfileDto updateProfileDto;

    @Captor
    private ArgumentCaptor<PagedRequestDto> pagedRequestCaptor;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();

        userId = UUID.randomUUID();
        userDto = UserDto.builder()
                .id(userId)
                .name("Carlos")
                .surname("González")
                .email("carlos@example.com")
                .role("ROLE_ADMIN")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        newUserDto = NewUserDto.builder()
                .email("newuser@example.com")
                .password("SecurePass2025!")
                .name("New")
                .surname("UserTest")
                .phoneNumber("+573001234567")
                .role("ROLE_SELLER")
                .build();

        updateProfileDto = UpdateUserProfileDto.builder()
                .name("Updated")
                .surname("Profile")
                .phoneNumber("+573001234568")
                .email("example@gmail.com")
                .build();
    }

    @Test
    void createUser_delegatesToService_andReturnsCreatedDto() throws Exception {
        when(userService.createUser(any(NewUserDto.class)))
                .thenReturn(ApiResponse.success("Created", userDto));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUserDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.email").value("carlos@example.com"));

        verify(userService, times(1)).createUser(any(NewUserDto.class));
    }

    @Test
    void updateUser_delegatesToService_andReturnsUpdatedDto() throws Exception {
        UserDto updated = UserDto.builder()
                .id(userId)
                .name("UpdatedName")
                .surname("UpdatedSurname")
                .email("updated@example.com")
                .build();

        when(userService.updateUser(eq(userId), any(NewUserDto.class)))
                .thenReturn(ApiResponse.success("Updated", updated));

        mockMvc.perform(put("/api/v1/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUserDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(userId.toString()))
                .andExpect(jsonPath("$.data.name").value("UpdatedName"));

        verify(userService, times(1)).updateUser(eq(userId), any(NewUserDto.class));
    }

    @Test
    void updateUserProfile_callsService_andReturnsDto() throws Exception {
        when(userService.updateUserProfile(any(UpdateUserProfileDto.class)))
                .thenReturn(ApiResponse.success("Updated profile", userDto));

        mockMvc.perform(put("/api/v1/users/update-user-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateProfileDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Updated profile"))
                .andExpect(jsonPath("$.data.email").value("carlos@example.com"));

        verify(userService, times(1)).updateUserProfile(any(UpdateUserProfileDto.class));
    }

    @Test
    void deactivateAndActivateUser_callsService_andReturnOk() throws Exception {
        when(userService.deactivateUser(userId)).thenReturn(ApiResponse.success("Deactivated", null));
        when(userService.activateUser(userId)).thenReturn(ApiResponse.success("Activated", null));

        mockMvc.perform(delete("/api/v1/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Deactivated"));

        mockMvc.perform(put("/api/v1/users/{id}/activate", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Activated"));

        verify(userService, times(1)).deactivateUser(userId);
        verify(userService, times(1)).activateUser(userId);
    }

    @Test
    void getUserById_andGetDeletedUserById_callsService() throws Exception {
        when(userService.getUserById(userId)).thenReturn(ApiResponse.success("Found", userDto));
        when(userService.getDeletedUserById(userId)).thenReturn(ApiResponse.success("Found deleted", userDto));

        mockMvc.perform(get("/api/v1/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(userId.toString()));

        mockMvc.perform(get("/api/v1/users/deleted/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Found deleted"));

        verify(userService, times(1)).getUserById(userId);
        verify(userService, times(1)).getDeletedUserById(userId);
    }

    @Test
    void listActiveAndInactiveUsers_bindPagedRequest_andCallService() throws Exception {
        when(userService.listActiveUsers(any(PagedRequestDto.class))).thenReturn(ApiResponse.success("OK", null));
        when(userService.listInactiveUsers(any(PagedRequestDto.class))).thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/users/active")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/inactive")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(userService, times(1)).listActiveUsers(pagedRequestCaptor.capture());
        verify(userService, times(1)).listInactiveUsers(any(PagedRequestDto.class));

        PagedRequestDto captured = pagedRequestCaptor.getValue();
        assertThat(captured.getPage()).isEqualTo(0);
        assertThat(captured.getSize()).isEqualTo(10);
    }

    @Test
    void getUserByEmail_andGetCurrentUser_andGetMetrics_callsService() throws Exception {
        when(userService.getUserByEmail("carlos@example.com")).thenReturn(ApiResponse.success("Found", userDto));
        when(userService.getCurrentUser()).thenReturn(ApiResponse.success("Current", userDto));
        MetricsDto metrics = MetricsDto.builder().totalProducts(5).build();
        when(userService.getMetrics()).thenReturn(ApiResponse.success("Metrics", metrics));

        mockMvc.perform(get("/api/v1/users/by-email")
                        .param("email", "carlos@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("carlos@example.com"));

        mockMvc.perform(get("/api/v1/users/current-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Current"));

        mockMvc.perform(get("/api/v1/users/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalProducts").value(5));

        verify(userService, times(1)).getUserByEmail("carlos@example.com");
        verify(userService, times(1)).getCurrentUser();
        verify(userService, times(1)).getMetrics();
    }

    @Test
    void changeNotificationsAndChatStatus_andChangeRole_callsService() throws Exception {
        when(userService.changeStatusNotification()).thenReturn(ApiResponse.success("Notifications toggled", null));
        when(userService.changeStatusNotificationChat()).thenReturn(ApiResponse.success("Chat toggled", null));
        when(userService.changeRole(userId, RoleList.ROLE_SELLER)).thenReturn(ApiResponse.success("Role changed", null));

        mockMvc.perform(put("/api/v1/users/change-notifications-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notifications toggled"));

        mockMvc.perform(put("/api/v1/users/change-chat-notifications-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Chat toggled"));

        mockMvc.perform(put("/api/v1/users/{id}/change-role", userId)
                        .param("role", "ROLE_SELLER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Role changed"));

        verify(userService, times(1)).changeStatusNotification();
        verify(userService, times(1)).changeStatusNotificationChat();
        verify(userService, times(1)).changeRole(userId, RoleList.ROLE_SELLER);
    }

    @Test
    void listUsersByName_andListInactiveUsersByName_callsService_withPagedRequest() throws Exception {
        when(userService.listUsersByName(any(PagedRequestDto.class))).thenReturn(ApiResponse.success("OK", null));
        when(userService.listInactiveUsersByName(any(PagedRequestDto.class))).thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/users/by-name")
                        .param("searchValue", "Carlos")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/inactive/search")
                        .param("searchValue", "Carlos")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(userService, times(1)).listUsersByName(any(PagedRequestDto.class));
        verify(userService, times(1)).listInactiveUsersByName(any(PagedRequestDto.class));
    }
}