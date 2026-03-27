package com.sigi.services;

import com.sigi.persistence.entity.ChatMessage;
import com.sigi.persistence.entity.ChatRoom;
import com.sigi.persistence.entity.User;
import com.sigi.persistence.repository.ChatMessageRepository;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.presentation.dto.websocket.chatmessage.ChatMessageDto;
import com.sigi.presentation.dto.websocket.chatmessage.NewChatMessageDto;
import com.sigi.services.service.websocket.chatmessage.ChatMessageServiceImpl;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceImplTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private DtoMapper dtoMapper;

    @Mock
    private PersistenceMethod persistenceMethod;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatMessageServiceImpl chatMessageService;

    private ChatRoom room;
    private User sender;
    private User participant;
    private ChatMessage message;
    private ChatMessageDto messageDto;
    private NewChatMessageDto newMessageDto;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        sender = User.builder().id(UUID.randomUUID()).email("sender@example.com").build();
        participant = User.builder().id(UUID.randomUUID()).email("other@example.com").build();

        room = ChatRoom.builder()
                .id(UUID.randomUUID())
                .participants(List.of(sender, participant))
                .build();

        message = ChatMessage.builder()
                .id(UUID.randomUUID())
                .room(room)
                .sender(sender)
                .content("Hello World")
                .isRead(false)
                .sentAt(LocalDateTime.now())
                .build();

        messageDto = ChatMessageDto.builder()
                .id(message.getId())
                .roomId(room.getId())
                .senderEmail(sender.getEmail())
                .content(message.getContent())
                .isRead(message.getIsRead())
                .sentAt(message.getSentAt())
                .build();

        newMessageDto = NewChatMessageDto.builder()
                .roomId(room.getId())
                .content("Hello World")
                .senderId(sender.getId())
                .build();

        meterRegistry = new SimpleMeterRegistry();
        chatMessageService = new ChatMessageServiceImpl(chatMessageRepository, dtoMapper, persistenceMethod, meterRegistry, messagingTemplate);

        lenient().when(dtoMapper.toChatMessageDto(any(ChatMessage.class))).thenReturn(messageDto);
        lenient().when(dtoMapper.toChatMessageDtoPage(any(Page.class))).thenAnswer(inv -> {
            Page<ChatMessage> page = inv.getArgument(0);
            return page.map(c -> messageDto);
        });
    }

    // ------------------- sendMessageToRoom -------------------
    @Test
    void shouldSendMessageToRoomSuccessfully_whenTwoParticipants() {
        when(persistenceMethod.getChatRoomById(room.getId())).thenReturn(room);
        when(persistenceMethod.getUserById(sender.getId())).thenReturn(sender);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(message);

        ApiResponse<ChatMessageDto> response = chatMessageService.sendMessageToRoom(newMessageDto);

        assertEquals(200, response.getCode());
        assertEquals("Message sent successfully", response.getMessage());
        assertEquals(messageDto, response.getData());
        verify(chatMessageRepository, times(1)).save(any(ChatMessage.class));
        verify(messagingTemplate, times(2)).convertAndSendToUser(anyString(), eq("/queue/chat.dm"), eq(messageDto));
        assertTrue(meterRegistry.get("chatMessage.service.operations").tags("type", "sendMessageToRoom").counter().count() >= 1.0);
    }

    @Test
    void shouldSendMessageToRoomToTopic_whenMoreThanTwoParticipants() {
        // make room with 3 participants
        User third = User.builder().id(UUID.randomUUID()).build();
        room.setParticipants(List.of(sender, participant, third));

        when(persistenceMethod.getChatRoomById(room.getId())).thenReturn(room);
        when(persistenceMethod.getUserById(sender.getId())).thenReturn(sender);
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(message);

        ApiResponse<ChatMessageDto> response = chatMessageService.sendMessageToRoom(newMessageDto);

        assertEquals(200, response.getCode());
        verify(messagingTemplate, times(1)).convertAndSend(eq("/topic/chat.room." + room.getId()), eq(messageDto));
    }

    @Test
    void shouldThrowWhenSenderNotInRoom() {
        User outsider = User.builder().id(UUID.randomUUID()).build();
        when(persistenceMethod.getChatRoomById(room.getId())).thenReturn(room);
        when(persistenceMethod.getUserById(outsider.getId())).thenReturn(outsider);

        NewChatMessageDto dto = NewChatMessageDto.builder()
                .roomId(room.getId())
                .content("Hi")
                .senderId(outsider.getId())
                .build();

        assertThrows(IllegalStateException.class, () -> chatMessageService.sendMessageToRoom(dto));
        verify(chatMessageRepository, never()).save(any());
    }

    // ------------------- listMessagesByRoom -------------------
    @Test
    void shouldListMessagesByRoomSuccessfully() {
        PagedRequestDto req = PagedRequestDto.builder()
                .page(0)
                .size(10)
                .searchId(room.getId())
                .sortDirection("asc")
                .build();

        Page<ChatMessage> page = new PageImpl<>(List.of(message), PageRequest.of(0,10, Sort.by("sentAt")), 1);
        when(chatMessageRepository.findByRoomId(eq(room.getId()), any(Pageable.class))).thenReturn(page);

        ApiResponse<PagedResponse<ChatMessageDto>> response = chatMessageService.listMessagesByRoom(req);

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().getTotalElements());
        assertEquals(messageDto, response.getData().getContent().get(0));
    }

    @Test
    void shouldReturnEmptyPageWhenNoMessagesInRoom() {
        PagedRequestDto req = PagedRequestDto.builder().page(0).size(10).searchId(room.getId()).sortDirection("desc").sortField("sentAt").build();
        Page<ChatMessage> empty = new PageImpl<>(List.of(), PageRequest.of(0,10), 0);
        when(chatMessageRepository.findByRoomId(eq(room.getId()), any(Pageable.class))).thenReturn(empty);

        ApiResponse<PagedResponse<ChatMessageDto>> response = chatMessageService.listMessagesByRoom(req);

        assertEquals(200, response.getCode());
        assertTrue(response.getData().getContent().isEmpty());
    }

    // ------------------- listMessagesBySender -------------------
    @Test
    void shouldListMessagesBySenderSuccessfully() {
        PagedRequestDto req = PagedRequestDto.builder().page(0).size(10).searchValue(sender.getEmail()).sortField("sentAt").sortDirection("desc").build();
        Page<ChatMessage> page = new PageImpl<>(List.of(message));
        when(chatMessageRepository.findBySenderEmail(eq(sender.getEmail()), any(Pageable.class))).thenReturn(page);

        ApiResponse<PagedResponse<ChatMessageDto>> response = chatMessageService.listMessagesBySender(req);

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().getTotalElements());
    }

    // ------------------- countUnreadMessages -------------------
    @Test
    void shouldCountUnreadMessagesSuccessfully() {
        when(persistenceMethod.getCurrentUser()).thenReturn(sender);
        when(chatMessageRepository.countByRoomIdAndIsReadFalseAndSenderIdNot(eq(room.getId()), eq(sender.getId()))).thenReturn(5L);

        ApiResponse<Long> response = chatMessageService.countUnreadMessages(room.getId());

        assertEquals(200, response.getCode());
        assertEquals(5L, response.getData());
    }

    // ------------------- markMessagesAsRead -------------------
    @Test
    void shouldMarkMessagesAsReadSuccessfully() {
        when(persistenceMethod.getCurrentUser()).thenReturn(sender);
        ChatMessage otherMsg = ChatMessage.builder().id(UUID.randomUUID()).sender(participant).isRead(false).build();
        Page<ChatMessage> page = new PageImpl<>(List.of(otherMsg));
        when(chatMessageRepository.findByRoomId(eq(room.getId()), eq(Pageable.unpaged()))).thenReturn(page);

        ApiResponse<Void> response = chatMessageService.markMessagesAsRead(room.getId());

        assertEquals(200, response.getCode());
        assertEquals("Messages marked as read", response.getMessage());
        assertTrue(otherMsg.getIsRead());
        verify(chatMessageRepository, times(1)).saveAll(anyList());
    }

    @Test
    void shouldHandleMarkMessagesAsReadWhenNoMessages() {
        when(persistenceMethod.getCurrentUser()).thenReturn(sender);
        Page<ChatMessage> empty = new PageImpl<>(List.of());
        when(chatMessageRepository.findByRoomId(eq(room.getId()), eq(Pageable.unpaged()))).thenReturn(empty);

        ApiResponse<Void> response = chatMessageService.markMessagesAsRead(room.getId());

        assertEquals(200, response.getCode());
    }
}