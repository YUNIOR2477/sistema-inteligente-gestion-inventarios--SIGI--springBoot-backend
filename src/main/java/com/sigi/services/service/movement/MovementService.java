package com.sigi.services.service.movement;

import com.sigi.presentation.dto.movement.MovementDto;
import com.sigi.presentation.dto.movement.NewMovementDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;

import java.util.UUID;

public interface MovementService {

    ApiResponse<MovementDto> updateMovement(UUID id, NewMovementDto dto);

    ApiResponse<Void> deleteMovement(UUID id);

    ApiResponse<Void> restoreMovement(UUID id);

    ApiResponse<MovementDto> getMovementById(UUID id);

    ApiResponse<MovementDto> getDeletedMovementById(UUID id);

    ApiResponse<PagedResponse<MovementDto>> listAllActiveMovements(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<MovementDto>> listAllDeletedMovements(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<MovementDto>> listMovementsByProduct(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<MovementDto>> listMovementsByOrder(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<MovementDto>> listMovementsByDispatcher(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<MovementDto>> listMovementsByType(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<MovementDto>> listDeletedMovementsByType(PagedRequestDto pagedRequestDto);

    // utility methods in the logic of other services and not in the controllers

    ApiResponse<MovementDto> createMovement(NewMovementDto dto);
}
