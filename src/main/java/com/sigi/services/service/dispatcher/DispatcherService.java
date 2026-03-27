package com.sigi.services.service.dispatcher;

import com.sigi.presentation.dto.dispatcher.DispatcherDto;
import com.sigi.presentation.dto.dispatcher.NewDispatcherDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;

import java.util.UUID;

public interface DispatcherService {
    ApiResponse<DispatcherDto> createDispatcher(NewDispatcherDto dto);

    ApiResponse<DispatcherDto> updateDispatcher(UUID id, NewDispatcherDto dto);

    ApiResponse<Void> deleteDispatcher(UUID id);

    ApiResponse<Void> restoreDispatcher(UUID id);

    ApiResponse<DispatcherDto> getDispatcherById(UUID id);

    ApiResponse<DispatcherDto> getDeletedDispatcherById(UUID id);

    ApiResponse<PagedResponse<DispatcherDto>> listAllActiveDispatchers(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<DispatcherDto>> listAllDeletedDispatchers(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<DispatcherDto>> listDispatcherByName( PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<DispatcherDto>> listDeletedDispatcherByName( PagedRequestDto pagedRequestDto);
}
