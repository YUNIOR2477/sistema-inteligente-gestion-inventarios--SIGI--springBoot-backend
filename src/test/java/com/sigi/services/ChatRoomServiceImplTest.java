package com.sigi.services;

import com.sigi.persistence.entity.ChatRoom;
import com.sigi.persistence.entity.User;
import com.sigi.persistence.repository.ChatRoomRepository;
import com.sigi.persistence.repository.UserRepository;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.presentation.dto.websocket.chatmessage.NewChatMessageDto;
import com.sigi.presentation.dto.websocket.chatroom.ChatRoomDto;
import com.sigi.presentation.dto.websocket.chatroom.NewChatRoomDto;
import com.sigi.services.service.websocket.chatmessage.ChatMessageService;
import com.sigi.services.service.websocket.chatroom.ChatRoomServiceImpl;
import com.sigi.util.DtoMapper;
import com.sigi.util.PersistenceMethod;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.sigi.util.Constants.*;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMessageService chatMessageService;

    @Mock
    private DtoMapper dtoMapper;

    @Mock
    private PersistenceMethod persistenceMethod;

    @InjectMocks
    private ChatRoomServiceImpl chatRoomService;

    private User currentUser;
    private User otherUser;
    private ChatRoom room;
    private NewChatRoomDto newRoomDto;
    private ChatRoomDto roomDto;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        currentUser = User.builder().id(UUID.randomUUID()).email("admin@example.com").build();
        otherUser = User.builder().id(UUID.randomUUID()).email("user@example.com").build();

        room = ChatRoom.builder()
                .id(UUID.randomUUID())
                .name("Team Room")
                .active(true)
                .admin(currentUser)
                .participants(new ArrayList<>(List.of(currentUser, otherUser)))
                .createdAt(LocalDateTime.now())
                .build();

        newRoomDto = NewChatRoomDto.builder()
                .name("Team Room")
                .participantIds(new ArrayList<>(List.of(otherUser.getId())))
                .build();

        roomDto = ChatRoomDto.builder()
                .id(room.getId())
                .name(room.getName())
                .active(room.getActive())
                .participantIds(new ArrayList<>(List.of(currentUser.getId(), otherUser.getId())))
                .createdAt(room.getCreatedAt())
                .build();

        meterRegistry = new SimpleMeterRegistry();
        chatRoomService = new ChatRoomServiceImpl(userRepository, chatRoomRepository, chatMessageService, meterRegistry, dtoMapper, persistenceMethod);

        // mapeos por defecto
        lenient().when(dtoMapper.toChatRoomDto(any(ChatRoom.class))).thenReturn(roomDto);
        lenient().when(dtoMapper.toChatRoomDtoPage(any(Page.class))).thenAnswer(inv -> {
            Page<ChatRoom> page = inv.getArgument(0);
            return page.map(c -> roomDto);
        });

    }

    // ------------------- createRoom -------------------
    @Test
    void shouldCreateRoomSuccessfully_andSendWelcomeMessage() {
        doReturn(new ArrayList<>(List.of(otherUser)))
                .when(userRepository).findAllById(anyList());

        doReturn(currentUser).when(persistenceMethod).getCurrentUser();

        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(inv -> {
            ChatRoom r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });

        ApiResponse<ChatRoomDto> response = chatRoomService.createRoom(newRoomDto);

        assertEquals(200, response.getCode());
        assertEquals(CREATED_SUCCESSFULLY.formatted(CHAT_ROOM), response.getMessage());
        verify(chatRoomRepository, times(1)).save(any(ChatRoom.class));
        verify(chatMessageService, times(1)).sendMessageToRoom(argThat(dto ->
                dto.getContent().contains("Welcome to the chat room")
        ));
        assertTrue(meterRegistry.get("chatRoom.service.operations").tags("type", "createRoom").counter().count() >= 1.0);
    }
    @Test
    void shouldThrowWhenCreatingRoomWithLessThanTwoParticipants() {
        // userRepository returns empty list, currentUser will be added -> only 1 participant
        when(userRepository.findAllById(anyList()))
                .thenReturn(List.of(otherUser));
        when(persistenceMethod.getCurrentUser()).thenReturn(currentUser);

        NewChatRoomDto dto = NewChatRoomDto.builder().name("Solo").participantIds(List.of()).build();

        assertThrows(UnsupportedOperationException.class, () -> chatRoomService.createRoom(dto));
        verify(chatRoomRepository, never()).save(any());
        verify(chatMessageService, never()).sendMessageToRoom(any());
    }

    // ------------------- addParticipants -------------------
    @Test
    void shouldAddParticipantsSuccessfully() {
        when(persistenceMethod.getChatRoomById(room.getId())).thenReturn(room);
        doReturn(List.of(otherUser)).when(userRepository).findAllById(anyList());
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(room);

        ApiResponse<Void> response = chatRoomService.addParticipants(room.getId(), List.of(otherUser.getId()));

        assertEquals(200, response.getCode());
        assertEquals("participants added successfully", response.getMessage());
        verify(chatRoomRepository, times(1)).save(room);
        ArgumentCaptor<List<UUID>> captor = ArgumentCaptor.forClass(List.class);
        verify(userRepository).findAllById(captor.capture());
        assertTrue(captor.getValue().contains(otherUser.getId()));
    }

    // ------------------- removeParticipant -------------------
    @Test
    void shouldRemoveParticipantSuccessfully() {
        when(persistenceMethod.getChatRoomById(room.getId())).thenReturn(room);
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(room);

        ApiResponse<Void> response = chatRoomService.removeParticipant(room.getId(), otherUser.getId());

        assertEquals(200, response.getCode());
        assertEquals("participant removed successfully", response.getMessage());
        assertFalse(room.getParticipants().stream().anyMatch(u -> u.getId().equals(otherUser.getId())));
        verify(chatRoomRepository, times(1)).save(room);
    }

    // ------------------- listRoomsByUser -------------------
    @Test
    void shouldListRoomsByUserSuccessfully() {
        PagedRequestDto req = PagedRequestDto.builder().page(0).size(10).sortDirection("desc").sortField("createdAt").build();
        Page<ChatRoom> page = new PageImpl<>(List.of(room), PageRequest.of(0,10), 1);
        when(persistenceMethod.getCurrentUser()).thenReturn(currentUser);
        when(chatRoomRepository.findByParticipants_Id(eq(currentUser.getId()), any(Pageable.class))).thenReturn(page);

        ApiResponse<PagedResponse<ChatRoomDto>> response = chatRoomService.listRoomsByUser(req);

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().getTotalElements());
        assertEquals(roomDto, response.getData().getContent().get(0));
    }

    // ------------------- listRoomsByName -------------------
    @Test
    void shouldListRoomsByNameSuccessfully() {
        PagedRequestDto req = PagedRequestDto.builder().page(0).size(10).searchValue("Team").sortDirection("desc").sortField("createdAt").build();
        Page<ChatRoom> page = new PageImpl<>(List.of(room));
        when(chatRoomRepository.findByNameContainingIgnoreCaseAndActiveTrue(eq("Team"), any(Pageable.class))).thenReturn(page);

        ApiResponse<PagedResponse<ChatRoomDto>> response = chatRoomService.listRoomsByName(req);

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().getTotalElements());
    }

    // ------------------- userInRoom -------------------
    @Test
    void shouldReturnUserInRoomSuccessfully() {
        when(persistenceMethod.getUserByEmail("admin@example.com")).thenReturn(currentUser);
        when(chatRoomRepository.existsByIdAndParticipants_Id(room.getId(), currentUser.getId())).thenReturn(true);

        ApiResponse<Boolean> response = chatRoomService.userInRoom("admin@example.com", room.getId());

        assertEquals(200, response.getCode());
        assertTrue(response.getData());
    }

    // ------------------- getRoomById -------------------
    @Test
    void shouldGetRoomByIdSuccessfully() {
        when(persistenceMethod.getChatRoomById(room.getId())).thenReturn(room);

        ApiResponse<ChatRoomDto> response = chatRoomService.getRoomById(room.getId());

        assertEquals(200, response.getCode());
        assertEquals(roomDto, response.getData());
    }

    // ------------------- deactivateRoom -------------------
    @Test
    void shouldDeactivateRoomWhenCurrentUserIsAdmin() {
        when(persistenceMethod.getChatRoomById(room.getId())).thenReturn(room);
        when(persistenceMethod.getCurrentUser()).thenReturn(currentUser);

        ApiResponse<Void> response = chatRoomService.deactivateRoom(room.getId());

        assertEquals(200, response.getCode());
        assertEquals("Room deactivated", response.getMessage());
        verify(chatRoomRepository, times(1)).delete(room);
    }

    @Test
    void shouldThrowWhenDeactivatingRoomIfNotAdmin() {
        when(persistenceMethod.getChatRoomById(room.getId())).thenReturn(room);
        when(persistenceMethod.getCurrentUser()).thenReturn(otherUser);

        assertThrows(IllegalArgumentException.class, () -> chatRoomService.deactivateRoom(room.getId()));
        verify(chatRoomRepository, never()).delete(any());
    }
}