package com.sigi.presentation.controller;

import com.sigi.persistence.enums.RoleList;
import com.sigi.presentation.dto.auth.NewUserDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.presentation.dto.user.MetricsDto;
import com.sigi.presentation.dto.user.UpdateUserProfileDto;
import com.sigi.presentation.dto.user.UserDto;
import com.sigi.services.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.sigi.util.Constants.*;

@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = OPERATION_COMPLETED,
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserDto.class)))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = INVALID_REQUEST,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = UNAUTHORIZED,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = INFO_NOT_FOUND,
        content = @Content(mediaType = ""))
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = UNEXPECTED_ERROR,
        content = @Content(mediaType = "application/json", schema = @Schema(ref = "#/components/schemas/GenericErrorResponse")))
@Tag(name = "User", description = "Endpoints for managing users")
@Validated
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    @Operation(summary = "Create a new user",
            description = "Creates a new user with the provided information.",
            operationId = "createUser",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping()
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody NewUserDto dto,
                                                           HttpServletRequest request) {
        log.info("(createUser) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<UserDto> response = userService.createUser(dto);
        log.info("(createUser) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update an existing user",
            description = "Updates the details of an existing user identified by their ID.",
            operationId = "updateUser",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(@PathVariable @NotNull UUID id,
                                                           @Valid @RequestBody NewUserDto dto,
                                                           HttpServletRequest request) {
        log.info("(updateUser) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<UserDto> response = userService.updateUser(id, dto);
        log.info("(updateUser) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update user profile",
            description = "Updates the details of user profile.",
            operationId = "updateUserProfile",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PutMapping("/update-user-profile")
    public ResponseEntity<ApiResponse<UserDto>> updateUserProfile(@Valid @RequestBody UpdateUserProfileDto dto,
                                                                  HttpServletRequest request) {
        log.info("(updateUserProfile) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<UserDto> response = userService.updateUserProfile(dto);
        log.info("(updateUserProfile) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Deactivate a user",
            description = "Deactivates the user identified by their ID.",
            operationId = "deactivateUser",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(@PathVariable @NotNull UUID id,
                                                            HttpServletRequest request) {
        log.info("(deactivateUser) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = userService.deactivateUser(id);
        log.info("(deactivateUser) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Activate a user",
            description = "Activates the user identified by their ID.",
            operationId = "activateUser",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<Void>> activateUser(@PathVariable @NotNull UUID id,
                                                          HttpServletRequest request) {
        log.info("(activateUser) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = userService.activateUser(id);
        log.info("(activateUser) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get user by ID",
            description = "Retrieves the user details identified by their ID.",
            operationId = "getUserById",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable @NotNull UUID id,
                                                            HttpServletRequest request) {
        log.info("(getUserById) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<UserDto> response = userService.getUserById(id);
        log.info("(getUserById) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get deleted user by ID",
            description = "Retrieves the deleted user information identified by the provided ID.",
            operationId = "getDeletedUserById",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")}
    )
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_AUDITOR')")
    @GetMapping("/deleted/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getDeletedUserById(@PathVariable @NotNull UUID id,
                                                                   HttpServletRequest request) {
        log.info("(getDeletedUserById) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<UserDto> response = userService.getDeletedUserById(id);
        log.info("(getDeletedUserById) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get active users",
            description = "Retrieves a paginated list of active users.",
            operationId = "listActiveUsers",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<PagedResponse<UserDto>>> listActiveUsers(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                               HttpServletRequest request) {
        log.info("(listActiveUsers) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<UserDto>> response = userService.listActiveUsers(pagedRequestDto);
        log.info("(listActiveUsers) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get inactive users",
            description = "Retrieves a paginated list of inactive users.",
            operationId = "listInactiveUsers",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/inactive")
    public ResponseEntity<ApiResponse<PagedResponse<UserDto>>> listInactiveUsers(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                 HttpServletRequest request) {
        log.info("(listInactiveUsers) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<UserDto>> response = userService.listInactiveUsers(pagedRequestDto);
        log.info("(listInactiveUsers) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get user by email",
            description = "Retrieves the user details identified by their email address.",
            operationId = "getUserByEmail",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @GetMapping("/by-email")
    public ResponseEntity<ApiResponse<UserDto>> getUserByEmail(@RequestParam @NotNull String email,
                                                               HttpServletRequest request) {
        log.info("(getUserByEmail) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<UserDto> response = userService.getUserByEmail(email);
        log.info("(getUserByEmail) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get currentUser",
            description = "Retrieves the current user details .",
            operationId = "getCurrentUser",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @GetMapping("/current-user")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(HttpServletRequest request) {
        log.info("(getCurrentUser) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<UserDto> response = userService.getCurrentUser();
        log.info("(getCurrentUser) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get metrics",
            description = "Retrieves the metrics details .",
            operationId = "getMetrics",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<MetricsDto>> getMetrics(HttpServletRequest request) {
        log.info("(getMetrics) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<MetricsDto> response = userService.getMetrics();
        log.info("(getMetrics) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Change Notifications status",
            description = "Change Notifications status true or false",
            operationId = "changeStatusNotification",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PutMapping("/change-notifications-status")
    public ResponseEntity<ApiResponse<Void>> changeStatusNotification(HttpServletRequest request) {
        log.info("(changeStatusNotification) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = userService.changeStatusNotification();
        log.info("(changeStatusNotification) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Change chat notifications status",
            description = "Change Chat Notifications status true or false",
            operationId = "changeChatNotificationStatus",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PutMapping("/change-chat-notifications-status")
    public ResponseEntity<ApiResponse<Void>> changeStatusChatNotifications(HttpServletRequest request) {
        log.info("(changeStatusChatNotifications) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = userService.changeStatusNotificationChat();
        log.info("(changeStatusChatNotifications) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Change user role",
            description = "Change user role by id",
            operationId = "changeUserRole",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PutMapping("/{id}/change-role")
    public ResponseEntity<ApiResponse<Void>> changeUserRole(@PathVariable @NotNull UUID id,
                                                            @RequestParam @NotNull RoleList role,
                                                            HttpServletRequest request) {
        log.info("(changeUserRole) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<Void> response = userService.changeRole(id, role);
        log.info("(changeUserRole) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get all users by name",
            description = "Retrieves a paginated list users filtered by name.",
            operationId = "listUsersByName",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @GetMapping("/by-name")
    public ResponseEntity<ApiResponse<PagedResponse<UserDto>>> listUsersByName(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                       HttpServletRequest request) {
        log.info("(listUsersByName) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<UserDto>> response = userService.listUsersByName(pagedRequestDto);
        log.info("(listUsersByName) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get inactive users by name",
            description = "Retrieves a paginated list of inactive users filtered by name.",
            operationId = "listInactiveUsersByName",
            security = {@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")})
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/inactive/search")
    public ResponseEntity<ApiResponse<PagedResponse<UserDto>>> listInactiveUsersByName(@ModelAttribute PagedRequestDto pagedRequestDto,
                                                                                       HttpServletRequest request) {
        log.info("(listInactiveUsersByName) -> " + REQUEST_RECEIVED, request.getMethod(), request.getRequestURI());
        ApiResponse<PagedResponse<UserDto>> response = userService.listInactiveUsersByName(pagedRequestDto);
        log.info("(listInactiveUsersByName) -> " + RESPONSE_SENT, response.getCode(), response.getMessage());
        return ResponseEntity.ok(response);
    }
}
