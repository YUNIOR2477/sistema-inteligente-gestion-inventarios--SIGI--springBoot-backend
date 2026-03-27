package com.sigi.services.service.warehouse;

import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.presentation.dto.warehouse.NewWarehouseDto;
import com.sigi.presentation.dto.warehouse.WarehouseDto;

import java.math.BigDecimal;
import java.util.UUID;

public interface WarehouseService {
    ApiResponse<WarehouseDto> createWarehouse(NewWarehouseDto dto);

    ApiResponse<WarehouseDto> updateWarehouse(UUID id, NewWarehouseDto dto);

    ApiResponse<Void> deleteWarehouse(UUID id);

    ApiResponse<Void> restoreWarehouse(UUID id);

    ApiResponse<WarehouseDto> getWarehouseById(UUID id);

    ApiResponse<WarehouseDto> getDeletedWarehouseById(UUID id);

    ApiResponse<PagedResponse<WarehouseDto>> listAllActiveWarehouse(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<WarehouseDto>> listAllDeletedWarehouse(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<WarehouseDto>> listWarehouseByCapacityGreaterOrEqual(Integer capacity, PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<WarehouseDto>> listWarehouseByName(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<WarehouseDto>> listDeletedWarehouseByName(PagedRequestDto pagedRequestDto);

    // utility method for other services logic, not for controllers
    ApiResponse<Boolean> validateAvailableCapacity(UUID warehouseId, BigDecimal incomingQuantity);

}
