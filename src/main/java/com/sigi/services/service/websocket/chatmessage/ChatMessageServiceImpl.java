package com.sigi.services.service.websocket.chatmessage;

import com.sigi.persistence.entity.ChatMessage;
import com.sigi.persistence.entity.ChatRoom;
import com.sigi.persistence.entity.User;
import com.sigi.persistence.repository.ChatMessageRepository;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.presentation.dto.websocket.chatmessage.ChatMessageDto;
import com.sigi.presentation.dto.websocket.chatmessage.NewChatMessageDto;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static com.sigi.util.Constants.*;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChatMessageServiceImpl implements ChatMessageService {
    private final ChatMessageRepository chatMessageRepository;
    private final DtoMapper dtoMapper;
    private final PersistenceMethod persistenceMethod;
    private final MeterRegistry meterRegistry;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    @CacheEvict(value = {CHAT_MESSAGE_BY_ROOM, CHAT_MESSAGE_BY_SENDER, CHAT_MESSAGE_BY_TYPE, CHAT_MESSAGE_COUNT_UNREAD}, allEntries = true)
    public ApiResponse<ChatMessageDto> sendMessageToRoom(NewChatMessageDto dto) {
        log.debug("(sendMessageToRoom) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        ChatRoom room = persistenceMethod.getChatRoomById(dto.getRoomId());
        User sender = persistenceMethod.getUserById(dto.getSenderId());
        if (!room.getParticipants().contains(sender)) {
            throw new IllegalStateException("You can't send messages in a chat you don't belong to");
        }
        ChatMessage message = ChatMessage.builder()
                .room(room)
                .sender(sender)
                .content(dto.getContent())
                .isRead(false)
                .sentAt(LocalDateTime.now(ZoneId.of("America/Bogota")))
                .build();
        ChatMessage saved = chatMessageRepository.save(message);
        ChatMessageDto payload = dtoMapper.toChatMessageDto(saved);
        if (room.getParticipants().size() == 2) {
            room.getParticipants().forEach(user -> {
                messagingTemplate.convertAndSendToUser(
                        user.getId().toString(),
                        "/queue/chat.dm",
                        payload
                );
            });
        } else {
            messagingTemplate.convertAndSend(
                    "/topic/chat.room." + room.getId(),
                    payload
            );
        }
        recordMetrics(sample, "sendMessageToRoom");
        log.info("(sendMessageToRoom) -> " + OPERATION_COMPLETED);
        return ApiResponse.success("Message sent successfully", payload);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CHAT_MESSAGE_BY_ROOM, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<ChatMessageDto>> listMessagesByRoom(PagedRequestDto pagedRequestDto) {
        log.debug("(listMessagesByRoom) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<ChatMessage> response = chatMessageRepository.findByRoomId(pagedRequestDto.getSearchId(), PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()),"sentAt")));
        log.info("(listMessagesByRoom) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "listMessagesByRoom");
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(CHAT_MESSAGES),
                dtoMapper.toChatMessageDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CHAT_MESSAGE_BY_SENDER, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<ChatMessageDto>> listMessagesBySender(PagedRequestDto pagedRequestDto) {
        log.debug("(listMessagesBySender) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<ChatMessage> response = chatMessageRepository.findBySenderEmail(pagedRequestDto.getSearchValue(), PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        log.info("(listMessagesBySender) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "listMessagesBySender");
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(CHAT_MESSAGES),
                dtoMapper.toChatMessageDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<Long> countUnreadMessages(UUID roomId) {
        log.debug("(countUnread) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        UUID userId = persistenceMethod.getCurrentUser().getId();
        long count = chatMessageRepository.countByRoomIdAndIsReadFalseAndSenderIdNot(roomId, userId);
        recordMetrics(sample, "countUnread");
        log.info("(countUnread) -> " + OPERATION_COMPLETED);
        return ApiResponse.success("Unread chats messages count", count);
    }

    @Override
    @Transactional
    @CacheEvict(value = {CHAT_MESSAGE_BY_ROOM,CHAT_ROOMS_BY_USER,CHAT_ROOM_BY_ID, CHAT_MESSAGE_BY_SENDER, CHAT_MESSAGE_BY_TYPE, CHAT_MESSAGE_COUNT_UNREAD}, allEntries = true)
    public ApiResponse<Void> markMessagesAsRead(UUID roomId) {
        log.debug("(markMessagesAsRead) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        UUID userId = persistenceMethod.getCurrentUser().getId();
        List<ChatMessage> messages = chatMessageRepository.findByRoomId(roomId, Pageable.unpaged()).getContent();
        messages.stream()
                .filter(m -> !m.getSender().getId().equals(userId))
                .forEach(m -> m.setIsRead(true));
        chatMessageRepository.saveAll(messages);
        recordMetrics(sample, "markMessagesAsRead");
        log.info("(markMessagesAsRead) -> " + OPERATION_COMPLETED);
        return ApiResponse.success("Messages marked as read", null);
    }

    private void recordMetrics(Timer.Sample sample, String operation) {
        sample.stop(Timer.builder("chatMessage.service.latency")
                .tag("operation", operation)
                .register(meterRegistry));
        meterRegistry.counter("chatMessage.service.operations", "type", operation).increment();
    }

}
