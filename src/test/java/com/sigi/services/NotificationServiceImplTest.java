package com.sigi.services;

import com.sigi.persistence.entity.Notification;
import com.sigi.persistence.entity.User;
import com.sigi.persistence.repository.NotificationRepository;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.presentation.dto.websocket.notification.NotificationDto;
import com.sigi.services.service.websocket.notification.NotificationServiceImpl;
import com.sigi.util.DtoMapper;
import com.sigi.util.PersistenceMethod;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private DtoMapper dtoMapper;

    @Mock
    private PersistenceMethod persistenceMethod;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private SimpleMeterRegistry meterRegistry;

    private UUID userId;
    private UUID notificationId;
    private User user;
    private Notification notification;
    private NotificationDto notificationDto;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        notificationService = new NotificationServiceImpl(notificationRepository, dtoMapper, meterRegistry, persistenceMethod, messagingTemplate);

        userId = UUID.randomUUID();
        notificationId = UUID.randomUUID();

        user = User.builder()
                .id(userId)
                .email("user@example.com")
                .build();

        notification = Notification.builder()
                .id(notificationId)
                .title("Test")
                .message("Hello")
                .user(user)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        notificationDto = NotificationDto.builder()
                .id(notificationId)
                .title("Test")
                .message("Hello")
                .isRead(false)
                .user(user.getEmail())
                .createdAt(notification.getCreatedAt())
                .build();

        lenient().when(dtoMapper.toNotificationDto(any(Notification.class))).thenReturn(notificationDto);
        lenient().when(dtoMapper.toNotificationDtoPage(any(Page.class))).thenAnswer(inv -> {
            Page<Notification> page = inv.getArgument(0);
            return page.map(n -> notificationDto);
        });
    }

    // ------------------- createNotification -------------------
    @Test
    void shouldCreateNotificationAndSendMessage() {
        when(persistenceMethod.getUserById(userId)).thenReturn(user);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(notificationId);
            return n;
        });

        notificationService.createNotification("Title", "Content", userId);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(captor.capture());
        Notification saved = captor.getValue();
        assertEquals("Title", saved.getTitle());
        assertEquals("Content", saved.getMessage());
        assertFalse(saved.getIsRead());
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/notifications/" + userId.toString()), any(Notification.class));
        assertTrue(meterRegistry.get("notification.service.operations").tags("type", "createNotification").counter().count() >= 1.0);
    }

    // ------------------- getNotificationById -------------------
    @Test
    void shouldReturnNotificationAndMarkAsReadWhenOwner() {
        when(persistenceMethod.getNotificationById(notificationId)).thenReturn(notification);
        when(persistenceMethod.getCurrentUserEmail()).thenReturn(user.getEmail());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        ApiResponse<NotificationDto> response = notificationService.getNotificationById(notificationId);

        assertEquals(200, response.getCode());
        assertEquals(notificationDto, response.getData());
        assertTrue(notification.getIsRead());
        verify(notificationRepository, times(1)).save(notification);
    }

    @Test
    void shouldThrowWhenGettingNotificationNotOwned() {
        User other = User.builder().id(UUID.randomUUID()).email("other@example.com").build();
        notification.setUser(other);
        when(persistenceMethod.getNotificationById(notificationId)).thenReturn(notification);
        when(persistenceMethod.getCurrentUserEmail()).thenReturn(user.getEmail());

        assertThrows(AccessDeniedException.class, () -> notificationService.getNotificationById(notificationId));
        verify(notificationRepository, never()).save(any());
    }

    // ------------------- listByUser / listUnreadByUser / countUnread -------------------
    @Test
    void shouldListByUserSuccessfully() {
        PagedRequestDto req = PagedRequestDto.builder().page(0).size(10).sortDirection("desc").sortField("createdAt").build();
        Page<Notification> page = new PageImpl<>(List.of(notification), PageRequest.of(0, 10), 1);
        when(persistenceMethod.getCurrentUser()).thenReturn(user);
        when(notificationRepository.findByUserId(eq(userId), any(Pageable.class))).thenReturn(page);

        ApiResponse<PagedResponse<NotificationDto>> response = notificationService.listByUser(req);

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().getTotalElements());
    }

    @Test
    void shouldListUnreadByUserSuccessfully() {
        PagedRequestDto req = PagedRequestDto.builder().page(0).size(10).sortDirection("desc").sortField("createdAt").build();
        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(persistenceMethod.getCurrentUser()).thenReturn(user);
        when(notificationRepository.findByUserIdAndIsReadFalse(eq(userId), any(Pageable.class))).thenReturn(page);

        ApiResponse<PagedResponse<NotificationDto>> response = notificationService.listUnreadByUser(req);

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().getTotalElements());
    }

    @Test
    void shouldCountUnreadSuccessfully() {
        when(persistenceMethod.getCurrentUser()).thenReturn(user);
        when(notificationRepository.countByUserIdAndIsReadFalse(userId)).thenReturn(3L);

        ApiResponse<Long> response = notificationService.countUnread();

        assertEquals(200, response.getCode());
        assertEquals(3L, response.getData());
    }

    // ------------------- listByTitle -------------------
    @Test
    void shouldListByTitleSuccessfully() {
        PagedRequestDto req = PagedRequestDto.builder().page(0).size(10).searchValue("Test").sortDirection("desc").sortField("createdAt").build();
        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(persistenceMethod.getCurrentUser()).thenReturn(user);
        when(notificationRepository.findByUserIdAndTitleContainingIgnoreCase(eq(userId), eq("Test"), any(Pageable.class))).thenReturn(page);

        ApiResponse<PagedResponse<NotificationDto>> response = notificationService.listByTitle(req);

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().getTotalElements());
    }

    // ------------------- markAsRead / markAllAsRead -------------------
    @Test
    void shouldMarkAsReadWhenOwner() {
        Notification n = Notification.builder().id(notificationId).user(user).isRead(false).build();
        when(persistenceMethod.getNotificationById(notificationId)).thenReturn(n);
        when(persistenceMethod.getCurrentUser()).thenReturn(user);
        when(notificationRepository.save(n)).thenReturn(n);

        ApiResponse<Void> response = notificationService.markAsRead(notificationId);

        assertEquals(200, response.getCode());
        assertEquals("Notification marked as read", response.getMessage());
        assertTrue(n.getIsRead());
        verify(notificationRepository, times(1)).save(n);
    }

    @Test
    void shouldThrowWhenMarkAsReadNotOwner() {
        User other = User.builder().id(UUID.randomUUID()).build();
        Notification n = Notification.builder().id(notificationId).user(other).isRead(false).build();
        when(persistenceMethod.getNotificationById(notificationId)).thenReturn(n);
        when(persistenceMethod.getCurrentUser()).thenReturn(user);

        assertThrows(IllegalStateException.class, () -> notificationService.markAsRead(notificationId));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void shouldMarkAllAsReadSuccessfully() {
        when(persistenceMethod.getCurrentUser()).thenReturn(user);
        Page<Notification> page = new PageImpl<>(List.of(notification));
        when(notificationRepository.findByUserIdAndIsReadFalse(userId, Pageable.unpaged())).thenReturn(page);
        when(notificationRepository.saveAll(anyList())).thenReturn(List.of(notification));

        ApiResponse<Void> response = notificationService.markAllAsRead();

        assertEquals(200, response.getCode());
        assertEquals("All notifications marked as read", response.getMessage());
        verify(notificationRepository, times(1)).saveAll(anyList());
        assertTrue(meterRegistry.get("notification.service.operations").tags("type", "markAllAsRead").counter().count() >= 1.0);
    }
}