package com.sigi.services.service.user;

import com.sigi.persistence.enums.RoleList;
import com.sigi.presentation.dto.auth.NewUserDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.presentation.dto.user.MetricsDto;
import com.sigi.presentation.dto.user.UpdateUserProfileDto;
import com.sigi.presentation.dto.user.UserDto;

import java.util.UUID;

public interface UserService {
    ApiResponse<UserDto> createUser(NewUserDto userDto);

    ApiResponse<UserDto> updateUser(UUID id, NewUserDto userDto);

    ApiResponse<UserDto> updateUserProfile(UpdateUserProfileDto userDto);

    ApiResponse<Void> deactivateUser(UUID id);

    ApiResponse<Void> activateUser(UUID id);

    ApiResponse<UserDto> getUserById(UUID id);

    ApiResponse<UserDto> getDeletedUserById(UUID id);

    ApiResponse<PagedResponse<UserDto>> listActiveUsers(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<UserDto>> listInactiveUsers(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<UserDto>> listUsersByName(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<UserDto>> listInactiveUsersByName(PagedRequestDto pagedRequestDto);

    ApiResponse<UserDto> getUserByEmail(String email);

    ApiResponse<UserDto> getCurrentUser();

    ApiResponse<Void> changeStatusNotification();

    ApiResponse<Void> changeStatusNotificationChat();

    ApiResponse<Void> changeRole(UUID id, RoleList role);

    ApiResponse<MetricsDto> getMetrics();
}
