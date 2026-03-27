package com.sigi.services.service.websocket.notification;

import com.sigi.persistence.entity.Notification;
import com.sigi.persistence.entity.User;
import com.sigi.persistence.repository.NotificationRepository;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.presentation.dto.websocket.notification.NotificationDto;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static com.sigi.util.Constants.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final DtoMapper dtoMapper;
    private final MeterRegistry meterRegistry;
    private final PersistenceMethod persistenceMethod;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    @CacheEvict(value = {NOTIFICATION_BY_ID, NOTIFICATIONS_BY_USER, NOTIFICATIONS_UNREAD, NOTIFICATIONS_BY_TYPE, NOTIFICATIONS_BY_SEVERITY}, allEntries = true)
    public void createNotification(String title, String content, UUID userId) {
        log.debug("(createNotification) -> Performing operation...");
        Timer.Sample sample = Timer.start(meterRegistry);
        User user = persistenceMethod.getUserById(userId);
        Notification notification = Notification.builder()
                .title(title)
                .message(content)
                .user(user)
                .isRead(false)
                .createdAt(LocalDateTime.now(ZoneId.of("America/Bogota")))
                .build();
        Notification response = notificationRepository.save(notification);

        String destination = "/topic/notifications/" + user.getId().toString();
        messagingTemplate.convertAndSend(destination, response);
        recordMetrics(sample, "createNotification");
        log.info("(createNotification) -> Operation completed");
    }

    @Override
    @Transactional
    @CacheEvict(value = {NOTIFICATION_BY_ID, NOTIFICATIONS_BY_USER, NOTIFICATIONS_UNREAD, NOTIFICATIONS_BY_TYPE, NOTIFICATIONS_BY_SEVERITY}, allEntries = true)
    public ApiResponse<NotificationDto> getNotificationById(UUID notificationId) {
        log.debug("(getNotificationById) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Notification response = persistenceMethod.getNotificationById(notificationId);
        if (!response.getUser().getEmail().equals(persistenceMethod.getCurrentUserEmail())) {
            throw new AccessDeniedException("You can't search for a response that isn't yours");
        }
        recordMetrics(sample, "getNotificationById");
        log.info("(getNotificationById) -> " + OPERATION_COMPLETED);
        if (Boolean.FALSE.equals(response.getIsRead())) {
            response.setIsRead(true);
            return ApiResponse.success(SEARCH_SUCCESSFULLY.formatted(NOTIFICATION),
                    dtoMapper.toNotificationDto(notificationRepository.save(response)));
        }
        return ApiResponse.success(SEARCH_SUCCESSFULLY.formatted(NOTIFICATION),
                dtoMapper.toNotificationDto(notificationRepository.save(response)));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = NOTIFICATIONS_BY_USER, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<NotificationDto>> listByUser(PagedRequestDto pagedRequestDto) {
        log.debug("(listMovementsByUser) -> " + PERFORMING_OPERATION);
        UUID userId = persistenceMethod.getCurrentUser().getId();
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Notification> response = notificationRepository
                .findByUserId(userId, PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        recordMetrics(sample, "listMovementsByUser");
        log.info("(listMovementsByUser) -> " + OPERATION_COMPLETED);
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(NOTIFICATIONS),
                dtoMapper.toNotificationDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = NOTIFICATIONS_UNREAD, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<NotificationDto>> listUnreadByUser(PagedRequestDto pagedRequestDto) {
        log.debug("(listUnreadByUser) -> " + PERFORMING_OPERATION);
        UUID userId = persistenceMethod.getCurrentUser().getId();
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Notification> response = notificationRepository
                .findByUserIdAndIsReadFalse(userId, PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        recordMetrics(sample, "listUnreadByUser");
        log.info("(listUnreadByUser) -> " + OPERATION_COMPLETED);
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(NOTIFICATIONS),
                dtoMapper.toNotificationDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<Long> countUnread() {
        log.debug("(countUnread) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        UUID userId = persistenceMethod.getCurrentUser().getId();
        long count = notificationRepository.countByUserIdAndIsReadFalse(userId);
        recordMetrics(sample, "countUnread");
        log.info("(countUnread) -> " + OPERATION_COMPLETED);
        return ApiResponse.success("Unread notifications count", count);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = NOTIFICATIONS_BY_TYPE, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<NotificationDto>> listByTitle(PagedRequestDto pagedRequestDto) {
        log.debug("(listMovementsByType) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        UUID userId = persistenceMethod.getCurrentUser().getId();
        Page<Notification> response = notificationRepository
                .findByUserIdAndTitleContainingIgnoreCase(userId, pagedRequestDto.getSearchValue(), PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        recordMetrics(sample, "listMovementsByType");
        log.info("(listMovementsByType) -> " + OPERATION_COMPLETED);
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(NOTIFICATION),
                dtoMapper.toNotificationDtoPage(response));
    }

    @Override
    @Transactional
    @CacheEvict(value = {NOTIFICATION_BY_ID, NOTIFICATIONS_BY_USER, NOTIFICATIONS_UNREAD, NOTIFICATIONS_BY_TYPE, NOTIFICATIONS_BY_SEVERITY}, allEntries = true)
    public ApiResponse<Void> markAsRead(UUID notificationId) {
        log.debug("(markAsRead) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Notification notification = persistenceMethod.getNotificationById(notificationId);
        UUID userId = persistenceMethod.getCurrentUser().getId();
        if (!notification.getUser().getId().equals(userId)) {
            throw new IllegalStateException("User not authorized to mark this notification");
        }
        notification.setIsRead(true);
        notificationRepository.save(notification);
        recordMetrics(sample, "markAsRead");
        log.debug("(markAsRead) -> " + PERFORMING_OPERATION);
        return ApiResponse.success("Notification marked as read", null);
    }

    @Override
    @Transactional
    @CacheEvict(value = {NOTIFICATION_BY_ID, NOTIFICATIONS_BY_USER, NOTIFICATIONS_UNREAD, NOTIFICATIONS_BY_TYPE, NOTIFICATIONS_BY_SEVERITY}, allEntries = true)
    public ApiResponse<Void> markAllAsRead() {
        log.debug("(markAllAsRead) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        UUID userId = persistenceMethod.getCurrentUser().getId();
        Page<Notification> notifications = notificationRepository
                .findByUserIdAndIsReadFalse(userId, Pageable.unpaged());
        notifications.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(notifications.getContent());
        recordMetrics(sample, "markAllAsRead");
        log.debug("(markAllAsRead) -> " + PERFORMING_OPERATION);
        return ApiResponse.success("All notifications marked as read", null);
    }

    private void recordMetrics(Timer.Sample sample, String operation) {
        sample.stop(Timer.builder("notification.service.latency")
                .tag("operation", operation)
                .register(meterRegistry));
        meterRegistry.counter("notification.service.operations", "type", operation).increment();
    }

}
