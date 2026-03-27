package com.sigi.services.service.websocket.notification;

import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.presentation.dto.websocket.notification.NotificationDto;

import java.util.UUID;

public interface NotificationService {
    void createNotification(String title, String content, UUID userId);

    ApiResponse<NotificationDto> getNotificationById(UUID notificationId);

    ApiResponse<Void> markAsRead(UUID notificationId);

    ApiResponse<Void> markAllAsRead();

    ApiResponse<PagedResponse<NotificationDto>> listUnreadByUser(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<NotificationDto>> listByUser(PagedRequestDto pagedRequestDto);

    ApiResponse<Long> countUnread();

    ApiResponse<PagedResponse<NotificationDto>> listByTitle(PagedRequestDto pagedRequestDto);

}
