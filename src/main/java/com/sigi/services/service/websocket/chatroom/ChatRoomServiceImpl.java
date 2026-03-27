package com.sigi.services.service.websocket.chatroom;

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
import com.sigi.util.DtoMapper;
import com.sigi.util.PersistenceMethod;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static com.sigi.util.Constants.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageService chatMessageService;
    private final MeterRegistry meterRegistry;
    private final DtoMapper dtoMapper;
    private final PersistenceMethod persistenceMethod;

    @Override
    @Transactional
    @CacheEvict(value = {CHAT_ROOM_BY_ID, CHAT_ROOMS_BY_USER}, allEntries = true)
    public ApiResponse<ChatRoomDto> createRoom(NewChatRoomDto dto) {
        log.debug("(createRoom) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        List<User> participants = userRepository.findAllById(dto.getParticipantIds());
        User currentUser = persistenceMethod.getCurrentUser();
        participants.add(currentUser);
        if (participants.size() < 2) {
            throw new IllegalArgumentException("The room must have at least two participant");
        }
        ChatRoom room = ChatRoom.builder()
                .name(dto.getName())
                .active(true)
                .admin(currentUser)
                .participants(participants)
                .createdAt(LocalDateTime.now(ZoneId.of("America/Bogota")))

                .build();
        ChatRoom response = chatRoomRepository.save(room);
        chatMessageService.sendMessageToRoom(NewChatMessageDto.builder()
                .roomId(response.getId())
                .content("Welcome to the chat room: " + response.getName())
                        .senderId(currentUser.getId())
                .build());
        recordMetrics(sample, "createRoom");
        log.info("(createRoom) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(CREATED_SUCCESSFULLY.formatted(CHAT_ROOM),
                dtoMapper.toChatRoomDto(response));
    }

    @Override
    @Transactional
    @CacheEvict(value = {CHAT_ROOM_BY_ID, CHAT_ROOMS_BY_USER}, allEntries = true)
    public ApiResponse<Void> addParticipants(UUID roomId, List<UUID> userIds) {
        log.debug("(addParticipants) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        ChatRoom room = persistenceMethod.getChatRoomById(roomId);
        List<User> newUsers = userRepository.findAllById(userIds);
        room.getParticipants().addAll(newUsers);
        chatRoomRepository.save(room);
        recordMetrics(sample, "addParticipants");
        log.info("(addParticipants) -> " + OPERATION_COMPLETED);
        return ApiResponse.success("participants added successfully", null);
    }

    @Override
    @Transactional
    @CacheEvict(value = {CHAT_ROOM_BY_ID, CHAT_ROOMS_BY_USER}, allEntries = true)
    public ApiResponse<Void> removeParticipant(UUID roomId, UUID userId) {
        log.debug("(removeParticipant) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        ChatRoom room = persistenceMethod.getChatRoomById(roomId);
        room.getParticipants().removeIf(u -> u.getId().equals(userId));
        chatRoomRepository.save(room);
        recordMetrics(sample, "removeParticipant");
        log.info("(removeParticipant) -> " + OPERATION_COMPLETED);
        return ApiResponse.success("participant removed successfully", null);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CHAT_ROOMS_BY_USER, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<ChatRoomDto>> listRoomsByUser(PagedRequestDto pagedRequestDto) {
        log.debug("(listRoomsByUser) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<ChatRoom> response = chatRoomRepository.findByParticipants_Id(persistenceMethod.getCurrentUser().getId(), PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        recordMetrics(sample, "listRoomsByUser");
        log.info("(listRoomsByUser) -> " + OPERATION_COMPLETED);
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(CHAT_ROOMS),
                dtoMapper.toChatRoomDtoPage(response));
    }

    @Override
    public ApiResponse<PagedResponse<ChatRoomDto>> listRoomsByName(PagedRequestDto pagedRequestDto) {
        log.debug("(listRoomsByName) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<ChatRoom> response = chatRoomRepository.findByNameContainingIgnoreCaseAndActiveTrue(pagedRequestDto.getSearchValue(), PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        recordMetrics(sample, "listRoomsByName");
        log.info("(listRoomsByName) -> " + OPERATION_COMPLETED);
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(CHAT_ROOMS),
                dtoMapper.toChatRoomDtoPage(response));
    }

    @Override
    @CacheEvict(value = {CHAT_ROOM_BY_ID, CHAT_ROOMS_BY_USER}, allEntries = true)
    public ApiResponse<Boolean> userInRoom(String username, UUID roomId) {
        log.debug("(userInRoom) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        UUID userId = persistenceMethod.getUserByEmail(username).getId();
        Boolean exist = chatRoomRepository.existsByIdAndParticipants_Id(roomId, userId);
        recordMetrics(sample, "userInRoom");
        log.info("(userInRoom) -> " + OPERATION_COMPLETED);
        return ApiResponse.success("successful validation: ", exist);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CHAT_ROOM_BY_ID, key = "#roomId")
    public ApiResponse<ChatRoomDto> getRoomById(UUID roomId) {
        log.debug("(getClientById) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        ChatRoom response = persistenceMethod.getChatRoomById(roomId);
        recordMetrics(sample, "getClientById");
        log.info("(getClientById) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(SEARCH_SUCCESSFULLY.formatted(CHAT_ROOMS),
                dtoMapper.toChatRoomDto(response));
    }

    @Override
    @Transactional
    @CacheEvict(value = {CHAT_ROOM_BY_ID, CHAT_ROOMS_BY_USER}, allEntries = true)
    public ApiResponse<Void> deactivateRoom(UUID roomId) {
        log.debug("(deactivateRoom) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        ChatRoom room = persistenceMethod.getChatRoomById(roomId);
        if(persistenceMethod.getCurrentUser() != room.getAdmin()){
            throw new IllegalArgumentException("You cannot delete a chat in which you are not the administrator");
        }
        chatRoomRepository.delete(room);
        recordMetrics(sample, "deactivateRoom");
        log.info("(deactivateRoom) -> " + OPERATION_COMPLETED);
        return ApiResponse.success("Room deactivated", null);
    }

    private void recordMetrics(Timer.Sample sample, String operation) {
        sample.stop(Timer.builder("chatRoom.service.latency")
                .tag("operation", operation)
                .register(meterRegistry));
        meterRegistry.counter("chatRoom.service.operations", "type", operation).increment();
    }
}
