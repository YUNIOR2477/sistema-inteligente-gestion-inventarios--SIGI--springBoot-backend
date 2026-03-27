package com.sigi.services.service.inventory;

import com.sigi.presentation.dto.inventory.*;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;

import java.math.BigDecimal;
import java.util.UUID;

public interface InventoryService {
    ApiResponse<InventoryDto> createInventory(NewInventoryDto dto);

    ApiResponse<InventoryDto> updateInventory(UUID id, NewInventoryDto dto);

    ApiResponse<Void> deleteInventory(UUID id);

    ApiResponse<Void> restoreInventory(UUID id);

    ApiResponse<InventoryDto> getInventoryById(UUID id);

    ApiResponse<InventoryDto> getDeletedInventoryById(UUID id);

    ApiResponse<PagedResponse<InventoryDto>> listAllInventories(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<InventoryDto>> listAllDeletedInventories(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<InventoryDto>> listInventoriesByWarehouse(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<InventoryDto>> listInventoriesByProduct(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<InventoryDto>> listInventoriesByLowStock(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<InventoryDto>> listAvailableInventoriesByWarehouse(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<InventoryDto>> listInventoriesByProductName(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<InventoryDto>> listInventoriesByProductSku(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<InventoryDto>> listDeletedInventoriesByProductName(PagedRequestDto pagedRequestDto);

    ApiResponse<Void> registerInventoryEntry(NewEntryDto newEntryDto);

    ApiResponse<Void> registerInventoryExit(UUID orderId);

    ApiResponse<Void> registerInventoryTransfer(ExitTransferDto exitTransferDto);

    ApiResponse<Void> registerInventoryExitForDisposal(ExitDisposalDto exitDisposalDto);

    // utility methods in the logic of other services and not in the controllers
    ApiResponse<Boolean> existsInventory(UUID productId, UUID warehouseId, String lot);

    ApiResponse<Void> reserveInventoryStock(UUID inventoryId, BigDecimal quantity, UUID orderId, UUID userId, String motive);

    ApiResponse<Void> releaseInventoryReservation(UUID inventoryId, BigDecimal quantity, UUID orderId, UUID userId, String motive);

    ApiResponse<Void> adjustInventoryStock(UUID inventoryId, BigDecimal newAvailableQuantity, BigDecimal newReservedQuantity, UUID userId, String motive);

}

