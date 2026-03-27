package com.sigi.presentation.controller;

import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.presentation.dto.websocket.notification.NotificationDto;
import com.sigi.services.service.websocket.notification.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.sigi.util.Constants.*;

@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = OPERATION_COMPLETED,
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = NotificationDto.class)))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = INVALID_REQUEST,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = UNAUTHORIZED,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = INFO_NOT_FOUND,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = UNEXPECTED_ERROR,
        content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/GenericErrorResponse")))
@Tag(name = "Notification Controller", description = "APIs for managing notifications")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
            summary = "Get notification by ID",
            description = "Retrieve a notification by its unique ID",
            operationId = "getNotificationById"
    )
    @GetMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<NotificationDto>> getNotificationById(@PathVariable @NotNull(message = ID_NOT_NULL) UUID notificationId,
                                                                            HttpServletRequest request) {
        log.info("(getNotificationById) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<NotificationDto> response = notificationService.getNotificationById(notificationId);
        log.info("(getNotificationById) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Mark notification as read",
            description = "Mark a specific notification as read by its unique ID",
            operationId = "markAsRead"
    )
    @PutMapping("/mark-as-read/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable @NotNull(message = ID_NOT_NULL) UUID notificationId,
                                                        HttpServletRequest request) {
        log.info("(markAsRead) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = notificationService.markAsRead(notificationId);
        log.info("(markAsRead) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Mark all notifications as read",
            description = "Mark all notifications for the current user as read",
            operationId = "markAllAsRead"
    )
    @PutMapping("/mark-all-as-read")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(HttpServletRequest request) {
        log.info("(markAllAsRead) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = notificationService.markAllAsRead();
        log.info("(markAllAsRead) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "List unread notifications",
            description = "Retrieve a paginated list of unread notifications for the current user",
            operationId = "listUnreadByUser"
    )
    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<PagedResponse<NotificationDto>>> listUnreadByUser(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                        HttpServletRequest request) {
        log.info("(listUnreadByUser) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<NotificationDto>> response = notificationService.listUnreadByUser(pagedRequestDto);
        log.info("(listUnreadByUser) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "List all notifications",
            description = "Retrieve a paginated list of all notifications for the current user",
            operationId = "listMovementsByUser"
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<NotificationDto>>> listByUser(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                  HttpServletRequest request) {
        log.info("(listMovementsByUser) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<NotificationDto>> response = notificationService.listByUser(pagedRequestDto);
        log.info("(listMovementsByUser) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Count unread notifications",
            description = "Get the count of unread notifications for the current user",
            operationId = "countUnread"
    )
    @GetMapping("/count-unread")
    public ResponseEntity<ApiResponse<Long>> countUnread(HttpServletRequest request) {
        log.info("(countUnread) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Long> response = notificationService.countUnread();
        log.info("(countUnread) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "List notifications by title",
            description = "Retrieve a paginated list of notifications filtered by title for the current user",
            operationId = "listByTitle"
    )
    @GetMapping("/by-tittle")
    public ResponseEntity<ApiResponse<PagedResponse<NotificationDto>>> listByTitle(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                   HttpServletRequest request) {
        log.info("(listByTitle) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<NotificationDto>> response = notificationService.listByTitle(pagedRequestDto);
        log.info("(listByTitle) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }
}
