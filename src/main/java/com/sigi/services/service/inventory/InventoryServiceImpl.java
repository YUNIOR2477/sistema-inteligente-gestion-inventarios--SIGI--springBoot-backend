package com.sigi.services.service.inventory;

import com.sigi.persistence.entity.*;
import com.sigi.persistence.enums.MovementType;
import com.sigi.persistence.enums.OrderStatus;
import com.sigi.persistence.enums.RoleList;
import com.sigi.persistence.repository.InventoryRepository;
import com.sigi.persistence.repository.MovementRepository;
import com.sigi.presentation.dto.inventory.*;
import com.sigi.presentation.dto.movement.NewMovementDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.services.service.movement.MovementService;
import com.sigi.services.service.warehouse.WarehouseService;
import com.sigi.services.service.websocket.notification.NotificationService;
import com.sigi.util.DtoMapper;
import com.sigi.util.PersistenceMethod;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.sigi.util.Constants.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final WarehouseService warehouseService;
    private final MovementService movementService;
    private final PersistenceMethod persistenceMethod;
    private final DtoMapper dtoMapper;
    private final MeterRegistry meterRegistry;
    private final NotificationService notificationService;

    @Override
    @Transactional
    @CacheEvict(value = {INVENTORY_BY_ID, INVENTORY_BY_PRODUCT_WAREHOUSE, INVENTORIES_BY_PRODUCT, INVENTORIES_BY_WAREHOUSE, INVENTORIES_BY_LOW_STOCK, INVENTORIES_BY_AVAILABLE_PRODUCT, ALL_ACTIVE_INVENTORIES, ALL_DELETED_INVENTORIES, INVENTORIES_DELETED_BY_NAME}, allEntries = true)
    public ApiResponse<InventoryDto> createInventory(NewInventoryDto dto) {
        log.debug("(createInventory) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Product product = persistenceMethod.getProductById(dto.getProductId());
        Warehouse warehouse = persistenceMethod.getWarehouseById(dto.getWarehouseId());
        boolean exists = inventoryRepository.existsByProductIdAndWarehouseIdAndLotAndActiveTrue(
                product.getId(), warehouse.getId(), dto.getLot());
        if (exists) {
            throw new IllegalArgumentException("Inventory already exists for product/warehouse/lot");
        }
        Inventory inv = Inventory.builder()
                .product(product)
                .warehouse(warehouse)
                .location(dto.getLocation())
                .lot(dto.getLot())
                .productionDate(dto.getProductionDate())
                .expirationDate(dto.getExpirationDate())
                .availableQuantity(dto.getAvailableQuantity() == null ? BigDecimal.ZERO : dto.getAvailableQuantity())
                .reservedQuantity(dto.getReservedQuantity() == null ? BigDecimal.ZERO : dto.getReservedQuantity())
                .active(true)
                .createdAt(LocalDateTime.now(ZoneId.of("America/Bogota")))
                .createdBy(persistenceMethod.getCurrentUserEmail())
                .build();
        Inventory saved;
        try {
            saved = inventoryRepository.save(inv);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            log.warn("(createInventory) -> DataIntegrityViolation while saving inventory, likely duplicate", ex);
            throw new IllegalArgumentException("Inventory already exists for product/warehouse/lot");
        }
        List<User> admins = persistenceMethod.getUsersByRoleName(RoleList.ROLE_ADMIN);
        List<User> warehouses = persistenceMethod.getUsersByRoleName(RoleList.ROLE_WAREHOUSE);
        String notificationText = "A new inventory for product " + product.getName() +
                " has been created in warehouse " + warehouse.getName() + " (Lot: " + dto.getLot() + ").";
        admins.forEach(user ->
                notificationService.createNotification(
                        "New inventory created",
                        notificationText,
                        user.getId()
                ));
        warehouses.forEach(user ->
                notificationService.createNotification(
                        "New inventory created",
                        notificationText,
                        user.getId()
                ));
        recordMetrics(sample, "createInventory");
        log.info("(createInventory) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(CREATED_SUCCESSFULLY.formatted(INVENTORY),
                dtoMapper.toInventoryDto(saved));
    }

    @Override
    @Transactional
    @CacheEvict(value = {INVENTORY_BY_ID, INVENTORY_BY_PRODUCT_WAREHOUSE, INVENTORIES_BY_PRODUCT, INVENTORIES_BY_WAREHOUSE, INVENTORIES_BY_LOW_STOCK, INVENTORIES_BY_AVAILABLE_PRODUCT, ALL_ACTIVE_INVENTORIES, ALL_DELETED_INVENTORIES, INVENTORIES_DELETED_BY_NAME}, allEntries = true)
    public ApiResponse<InventoryDto> updateInventory(UUID id, NewInventoryDto dto) {
        log.debug("(updateInventory) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Inventory existing = persistenceMethod.getInventoryById(id);
        if (Boolean.FALSE.equals(existing.getActive())) {
            throw new IllegalArgumentException(ENTITY_INACTIVE.formatted(INVENTORY_ID.formatted(id)));
        }
        boolean identityChanged = !existing.getProduct().getId().equals(dto.getProductId())
                || !existing.getWarehouse().getId().equals(dto.getWarehouseId())
                || !Objects.equals(existing.getLot(), dto.getLot());
        if (identityChanged) {
            Product product = persistenceMethod.getProductById(dto.getProductId());
            Warehouse warehouse = persistenceMethod.getWarehouseById(dto.getWarehouseId());
            boolean exists = inventoryRepository.existsByProductIdAndWarehouseIdAndLotAndActiveTrue(
                    product.getId(), warehouse.getId(), dto.getLot());
            if (exists && !(existing.getProduct().getId().equals(product.getId())
                    && existing.getWarehouse().getId().equals(warehouse.getId())
                    && Objects.equals(existing.getLot(), dto.getLot()))) {
                throw new IllegalArgumentException("Another active inventory already exists for product/warehouse/lot");
            }
            existing.setProduct(product);
            existing.setWarehouse(warehouse);
            existing.setLot(dto.getLot());
        } else {
            existing.setLot(dto.getLot());
        }
        existing.setLocation(dto.getLocation());
        existing.setProductionDate(dto.getProductionDate());
        existing.setExpirationDate(dto.getExpirationDate());
        if (dto.getAvailableQuantity() != null) {
            if (dto.getAvailableQuantity().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Available quantity cannot be negative");
            }
            existing.setAvailableQuantity(dto.getAvailableQuantity());
        }
        if (dto.getReservedQuantity() != null) {
            if (dto.getReservedQuantity().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Reserved quantity cannot be negative");
            }
            existing.setReservedQuantity(dto.getReservedQuantity());
        }
        BigDecimal available = existing.getAvailableQuantity() == null ? BigDecimal.ZERO : existing.getAvailableQuantity();
        BigDecimal reserved = existing.getReservedQuantity() == null ? BigDecimal.ZERO : existing.getReservedQuantity();
        if (reserved.compareTo(available) > 0) {
            throw new IllegalArgumentException("Reserved quantity cannot exceed available quantity");
        }
        existing.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        existing.setUpdatedBy(persistenceMethod.getCurrentUserEmail());
        Inventory updated;
        try {
            updated = inventoryRepository.save(existing);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            log.warn("(updateInventory) -> DataIntegrityViolation while updating inventory id: {}", id, ex);
            throw new IllegalArgumentException("Another active inventory already exists for product/warehouse/lot");
        }
        Product finalProduct = updated.getProduct();
        Warehouse finalWarehouse = updated.getWarehouse();
        String notificationText = "The inventory for product " + finalProduct.getName() +
                " has been updated in warehouse " + finalWarehouse.getName() + " (Lot: " + updated.getLot() + ").";
        List<User> admins = persistenceMethod.getUsersByRoleName(RoleList.ROLE_ADMIN);
        List<User> warehouses = persistenceMethod.getUsersByRoleName(RoleList.ROLE_WAREHOUSE);
        admins.forEach(user ->
                notificationService.createNotification(
                        "Inventory updated",
                        notificationText,
                        user.getId()
                ));
        warehouses.forEach(user ->
                notificationService.createNotification(
                        "Inventory updated",
                        notificationText,
                        user.getId()
                ));
        recordMetrics(sample, "updateInventory");
        log.info("(updateInventory) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(UPDATED_SUCCESSFULLY.formatted(INVENTORY),
                dtoMapper.toInventoryDto(updated));
    }

    @Override
    @Transactional
    @CacheEvict(value = {INVENTORY_BY_ID, INVENTORY_BY_PRODUCT_WAREHOUSE, INVENTORIES_BY_PRODUCT, INVENTORIES_BY_WAREHOUSE, INVENTORIES_BY_LOW_STOCK, INVENTORIES_BY_AVAILABLE_PRODUCT, ALL_ACTIVE_INVENTORIES, ALL_DELETED_INVENTORIES, INVENTORIES_DELETED_BY_NAME}, allEntries = true)
    public ApiResponse<Void> deleteInventory(UUID id) {
        log.debug("(deleteInventory) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Inventory existingInventory = persistenceMethod.getInventoryById(id);
        if (Boolean.FALSE.equals(existingInventory.getActive())) {
            throw new IllegalArgumentException(ENTITY_PREVIOUSLY_DELETED.formatted(INVENTORY_ID.formatted(id)));
        }
        BigDecimal available = existingInventory.getAvailableQuantity() == null ? BigDecimal.ZERO : existingInventory.getAvailableQuantity();
        BigDecimal reserved = existingInventory.getReservedQuantity() == null ? BigDecimal.ZERO : existingInventory.getReservedQuantity();
        if (available.compareTo(BigDecimal.ZERO) > 0 || reserved.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("Cannot delete inventory with available or reserved stock");
        }
        existingInventory.setActive(false);
        existingInventory.setDeletedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        existingInventory.setDeletedBy(persistenceMethod.getCurrentUserEmail());
        existingInventory.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        existingInventory.setUpdatedBy(persistenceMethod.getCurrentUserEmail());
        try {
            inventoryRepository.save(existingInventory);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            log.warn("(deleteInventory) -> DataIntegrityViolation while deleting inventory id: {}", id, ex);
            throw new IllegalArgumentException("Unable to delete inventory due to data integrity constraints");
        }
        String notificationText = "The inventory for product " + existingInventory.getProduct().getName() +
                " has been deleted from warehouse " + existingInventory.getWarehouse().getName() +
                " (Lot: " + existingInventory.getLot() + ").";

        List<User> admins = persistenceMethod.getUsersByRoleName(RoleList.ROLE_ADMIN);
        List<User> warehouses = persistenceMethod.getUsersByRoleName(RoleList.ROLE_WAREHOUSE);
        admins.forEach(user ->
                notificationService.createNotification(
                        "Inventory deleted",
                        notificationText,
                        user.getId()
                ));
        warehouses.forEach(user ->
                notificationService.createNotification(
                        "Inventory deleted",
                        notificationText,
                        user.getId()
                ));

        recordMetrics(sample, "deleteInventory");
        log.info("(deleteInventory) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(DELETED_SUCCESSFULLY.formatted(INVENTORY), null);
    }

    @Override
    @Transactional
    @CacheEvict(value = {INVENTORY_BY_ID, INVENTORY_BY_PRODUCT_WAREHOUSE, INVENTORIES_BY_PRODUCT, INVENTORIES_BY_WAREHOUSE, INVENTORIES_BY_LOW_STOCK, INVENTORIES_BY_AVAILABLE_PRODUCT, ALL_ACTIVE_INVENTORIES, ALL_DELETED_INVENTORIES, INVENTORIES_DELETED_BY_NAME}, allEntries = true)
    public ApiResponse<Void> restoreInventory(UUID id) {
        log.debug("(restoreInventory) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Inventory inv = persistenceMethod.getInventoryById(id);
        if (Boolean.TRUE.equals(inv.getActive()))
            return ApiResponse.success(ENTITY_ACTIVATED_SUCCESSFULLY.formatted(INVENTORY_ID.formatted(id)), null);
        boolean conflict = inventoryRepository.existsByProductIdAndWarehouseIdAndLotAndActiveTrue(
                inv.getProduct().getId(), inv.getWarehouse().getId(), inv.getLot());
        if (conflict) {
            throw new IllegalStateException("Cannot restore inventory: active inventory with same product/warehouse/lot exists");
        }
        BigDecimal incoming = inv.getAvailableQuantity().add(inv.getReservedQuantity());
        if (Boolean.FALSE.equals(warehouseService.validateAvailableCapacity(inv.getWarehouse().getId(), incoming).getData())) {
            throw new IllegalStateException("Cannot restore inventory: warehouse capacity exceeded");
        }
        inv.setActive(true);
        inv.setDeletedAt(null);
        inv.setDeletedBy(null);
        inv.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        inv.setUpdatedBy(persistenceMethod.getCurrentUserEmail());
        inventoryRepository.save(inv);
        List<User> admin = persistenceMethod.getUsersByRoleName(RoleList.ROLE_ADMIN);
        List<User> warehouse = persistenceMethod.getUsersByRoleName(RoleList.ROLE_WAREHOUSE);
        admin.forEach(user ->
                notificationService.createNotification(
                        "Inventory restored",
                        "The inventory for product " + inv.getProduct().getName() +
                                " has been restored in warehouse " + inv.getWarehouse().getName() + " (Lot: " + inv.getLot() + ").",
                        user.getId()
                ));
        warehouse.forEach(user ->
                notificationService.createNotification(
                        "Inventory restored",
                        "The inventory for product " + inv.getProduct().getName() +
                                " has been restored in warehouse " + inv.getWarehouse().getName() + " (Lot: " + inv.getLot() + ").",
                        user.getId()
                ));
        recordMetrics(sample, "restoreInventory");
        log.info("(restoreInventory) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(OPERATION_COMPLETED_SUCCESSFULLY.formatted("Inventory restored"), null);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = INVENTORY_BY_ID, key = "#id")
    public ApiResponse<InventoryDto> getInventoryById(UUID id) {
        log.debug("(getInventoryById) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Inventory response = persistenceMethod.getInventoryById(id);
        if (response.getActive().equals(Boolean.FALSE)) {
            throw new IllegalArgumentException(ENTITY_INACTIVE.formatted(INVENTORY_ID.formatted(id)));
        }
        recordMetrics(sample, "getInventoryById");
        log.info("(getInventoryById) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(SEARCH_SUCCESSFULLY.formatted(INVENTORY),
                dtoMapper.toInventoryDto(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = INVENTORY_BY_ID, key = "#id")
    public ApiResponse<InventoryDto> getDeletedInventoryById(UUID id) {
        log.debug("(getDeletedInventoryById) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Inventory response = inventoryRepository.findByIdAndActiveFalse(id).orElseThrow(() -> new EntityNotFoundException(ENTITY_NOT_FOUND.formatted(INVENTORY_ID.formatted(id))));
        recordMetrics(sample, "getDeletedInventoryById");
        log.info("(getDeletedInventoryById) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(SEARCH_SUCCESSFULLY.formatted(INVENTORY),
                dtoMapper.toInventoryDto(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ALL_ACTIVE_INVENTORIES, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<InventoryDto>> listAllInventories(PagedRequestDto pagedRequestDto) {
        log.debug("(listAllInventories) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Inventory> response = inventoryRepository.findByActiveTrue(PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        recordMetrics(sample, "listAllInventories");
        log.info("(listAllInventories) -> " + OPERATION_COMPLETED);
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(INVENTORIES),
                dtoMapper.toInventoryDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ALL_DELETED_INVENTORIES, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<InventoryDto>> listAllDeletedInventories(PagedRequestDto pagedRequestDto) {
        log.debug("(listAllDeletedInventories) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Inventory> response = inventoryRepository.findByActiveFalse(PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        recordMetrics(sample, "listAllDeletedInventories");
        log.info("(listAllDeletedInventories) -> " + OPERATION_COMPLETED);
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted("deleted inventories"),
                dtoMapper.toInventoryDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = INVENTORIES_BY_WAREHOUSE, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<InventoryDto>> listInventoriesByWarehouse(PagedRequestDto pagedRequestDto) {
        log.debug("(listInventoriesByWarehouse) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Inventory> response = inventoryRepository.findByWarehouseNameContainingIgnoreCaseAndActiveTrue(pagedRequestDto.getSearchValue(), PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        recordMetrics(sample, "listInventoriesByWarehouse");
        log.info("(listInventoriesByWarehouse) -> " + OPERATION_COMPLETED);
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(INVENTORIES),
                dtoMapper.toInventoryDtoPage(response));

    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = INVENTORIES_BY_PRODUCT, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<InventoryDto>> listInventoriesByProduct(PagedRequestDto pagedRequestDto) {
        log.debug("(listInventoriesByProduct) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Inventory> response = inventoryRepository.findByProductIdAndActiveTrue(pagedRequestDto.getSearchId(), PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        recordMetrics(sample, "listInventoriesByProduct");
        log.info("(listInventoriesByProduct) -> " + OPERATION_COMPLETED);
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(INVENTORIES),
                dtoMapper.toInventoryDtoPage(response));

    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = INVENTORIES_BY_LOW_STOCK, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<InventoryDto>> listInventoriesByLowStock(PagedRequestDto pagedRequestDto) {
        log.debug("(listInventoriesByLowStock) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Inventory> response = inventoryRepository.findLowStockAndActiveTrue(pagedRequestDto.getSearchNumber(), PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        recordMetrics(sample, "listInventoriesByLowStock");
        log.info("(listInventoriesByLowStock) -> " + OPERATION_COMPLETED);
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(INVENTORIES),
                dtoMapper.toInventoryDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = INVENTORIES_BY_AVAILABLE_PRODUCT, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<InventoryDto>> listAvailableInventoriesByWarehouse(PagedRequestDto pagedRequestDto) {
        log.debug("(listAvailableProductsInWarehouse) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Inventory> response = inventoryRepository.findByWarehouseNameContainingIgnoreCaseAndActiveTrue(pagedRequestDto.getSearchValue(), PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        recordMetrics(sample, "listAvailableInventoriesByWarehouse");
        log.info("(listAvailableProductsInWarehouse) -> " + OPERATION_COMPLETED);
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(INVENTORIES),
                dtoMapper.toInventoryDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<PagedResponse<InventoryDto>> listInventoriesByProductName(PagedRequestDto pagedRequestDto) {
        log.debug("(listInventoriesByProductName) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Inventory> response = inventoryRepository.findByProductNameContainingIgnoreCaseAndActiveTrue(pagedRequestDto.getSearchValue(), PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        recordMetrics(sample, "listInventoriesByProductName");
        log.info("(listInventoriesByProductName) -> " + OPERATION_COMPLETED);
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted("Inventories with product name containing '%s'".formatted(pagedRequestDto.getSearchValue())),
                dtoMapper.toInventoryDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<PagedResponse<InventoryDto>> listInventoriesByProductSku(PagedRequestDto pagedRequestDto) {
        log.debug("(listInventoriesByProductSku) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Inventory> response = inventoryRepository
                .findByProductSkuContainingIgnoreCaseAndActiveTrueAndIdNot(
                        pagedRequestDto.getSearchValue(),
                        pagedRequestDto.getSearchId(),
                        PageRequest.of(
                                pagedRequestDto.getPage(),
                                pagedRequestDto.getSize(),
                                Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())
                        )
                );
        recordMetrics(sample, "listInventoriesByProductSku");
        log.info("(listInventoriesByProductSku) -> " + OPERATION_COMPLETED);
        return ApiResponse.successPage(
                SEARCH_SUCCESSFULLY.formatted("Inventories with product sku: '%s'".formatted(pagedRequestDto.getSearchValue())),
                dtoMapper.toInventoryDtoPage(
                        response
                )
        );
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = INVENTORIES_DELETED_BY_NAME, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<InventoryDto>> listDeletedInventoriesByProductName(PagedRequestDto pagedRequestDto) {
        log.debug("(listDeletedInventoriesByProductName) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Inventory> response = inventoryRepository.findByProductNameContainingIgnoreCaseAndActiveFalse(pagedRequestDto.getSearchValue(), PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        recordMetrics(sample, "listDeletedInventoriesByProductName");
        log.info("(listDeletedInventoriesByProductName) -> " + OPERATION_COMPLETED);
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted("deleted inventories with product name containing '%s'".formatted(pagedRequestDto.getSearchValue())),
                dtoMapper.toInventoryDtoPage(response));
    }

    @Override
    @Transactional
    @CacheEvict(value = {INVENTORY_BY_ID, INVENTORY_BY_PRODUCT_WAREHOUSE, INVENTORIES_BY_PRODUCT, INVENTORIES_BY_WAREHOUSE, INVENTORIES_BY_LOW_STOCK, INVENTORIES_BY_AVAILABLE_PRODUCT, ALL_ACTIVE_INVENTORIES, ALL_DELETED_INVENTORIES, INVENTORIES_DELETED_BY_NAME}, allEntries = true)
    public ApiResponse<Void> registerInventoryEntry(NewEntryDto newEntryDto) {
        log.debug("(registerInventoryEntry) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        if (newEntryDto.getQuantity() == null || newEntryDto.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        Inventory inventory = persistenceMethod.getInventoryById(newEntryDto.getInventoryId());
        if (Boolean.FALSE.equals(inventory.getActive())) {
            throw new IllegalArgumentException("Inventory is inactive");
        }
        ApiResponse<Boolean> capacityCheck = warehouseService.validateAvailableCapacity(inventory.getWarehouse().getId(), newEntryDto.getQuantity());
        if (Boolean.FALSE.equals(capacityCheck.getData())) {
            throw new IllegalArgumentException("Warehouse capacity exceeded");
        }
        final int MAX_RETRIES = 3;
        int attempt = 0;
        while (true) {
            try {
                BigDecimal currentAvailable = inventory.getAvailableQuantity() == null ? BigDecimal.ZERO : inventory.getAvailableQuantity();
                inventory.setAvailableQuantity(currentAvailable.add(newEntryDto.getQuantity()));
                inventory.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
                inventory.setUpdatedBy(persistenceMethod.getCurrentUserEmail());
                inventoryRepository.save(inventory);
                NewMovementDto movementDto = NewMovementDto.builder()
                        .type(MovementType.ENTRY)
                        .inventoryId(inventory.getId())
                        .productId(inventory.getProduct().getId())
                        .quantity(newEntryDto.getQuantity())
                        .dispatcherId(newEntryDto.getDispatcherId())
                        .motive(newEntryDto.getMotive())
                        .build();
                movementService.createMovement(movementDto);
                String message = "An entry of " + newEntryDto.getQuantity() + " units of product " + inventory.getProduct().getName() +
                        " has been registered in warehouse " + inventory.getWarehouse().getName() + " (Lot: " + inventory.getLot() + ").";
                List<User> auditors = persistenceMethod.getUsersByRoleName(RoleList.ROLE_AUDITOR);
                List<User> admins = persistenceMethod.getUsersByRoleName(RoleList.ROLE_ADMIN);
                List<User> warehouses = persistenceMethod.getUsersByRoleName(RoleList.ROLE_WAREHOUSE);
                auditors.forEach(user -> notificationService.createNotification("Entry registered", message, user.getId()));
                admins.forEach(user -> notificationService.createNotification("Entry registered", message, user.getId()));
                warehouses.forEach(user -> notificationService.createNotification("Entry registered", message, user.getId()));
                recordMetrics(sample, "registerInventoryEntry");
                log.info("(registerInventoryEntry) -> " + OPERATION_COMPLETED);
                return ApiResponse.success(OPERATION_COMPLETED_SUCCESSFULLY.formatted("registered entry"), null);
            } catch (org.springframework.dao.OptimisticLockingFailureException ex) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    log.error("(registerInventoryEntry) -> Optimistic lock failure after {} attempts for inventory {}", attempt, inventory.getId(), ex);
                    throw ex;
                }
                inventory = inventoryRepository.findById(inventory.getId())
                        .orElseThrow(() -> new IllegalArgumentException("Inventory not found during retry"));
            }
        }
    }

    @Override
    @Transactional
    @CacheEvict(value = {
            INVENTORY_BY_ID, INVENTORY_BY_PRODUCT_WAREHOUSE, INVENTORIES_BY_PRODUCT,
            INVENTORIES_BY_WAREHOUSE, INVENTORIES_BY_LOW_STOCK, INVENTORIES_BY_AVAILABLE_PRODUCT,
            ALL_ACTIVE_INVENTORIES, ALL_DELETED_INVENTORIES, INVENTORIES_DELETED_BY_NAME
    }, allEntries = true)
    public ApiResponse<Void> registerInventoryExit(UUID orderId) {
        log.debug("(registerInventoryExit) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);

        Order order = persistenceMethod.getOrderById(orderId);
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new IllegalStateException("Order must be delivered to register exit");
        }

        for (OrderLine line : order.getLines()) {
            Inventory inventory = line.getInventory();
            BigDecimal quantity = line.getQuantity();

            BigDecimal reserved = inventory.getReservedQuantity() == null ? BigDecimal.ZERO : inventory.getReservedQuantity();
            BigDecimal available = inventory.getAvailableQuantity() == null ? BigDecimal.ZERO : inventory.getAvailableQuantity();

            if (reserved.compareTo(quantity) >= 0) {
                inventory.setReservedQuantity(reserved.subtract(quantity));
            } else {
                BigDecimal remaining = quantity.subtract(reserved);
                inventory.setReservedQuantity(BigDecimal.ZERO);
                if (available.compareTo(remaining) < 0) {
                    throw new IllegalArgumentException("Insufficient stock for product " + inventory.getProduct().getName());
                }
                inventory.setAvailableQuantity(available.subtract(remaining));
            }

            inventory.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
            inventory.setUpdatedBy(persistenceMethod.getCurrentUserEmail());
            inventoryRepository.save(inventory);

            NewMovementDto movementDto = NewMovementDto.builder()
                    .type(MovementType.EXIT)
                    .inventoryId(inventory.getId())
                    .productId(inventory.getProduct().getId())
                    .quantity(quantity)
                    .orderId(order.getId())
                    .dispatcherId(order.getDispatcher().getId())
                    .motive("Order dispatched")
                    .build();
            movementService.createMovement(movementDto);
        }

        String message = "Exit registered for order " + order.getId() + " with " + order.getLines().size() + " lines.";
        List<User> auditors = persistenceMethod.getUsersByRoleName(RoleList.ROLE_AUDITOR);
        List<User> admins = persistenceMethod.getUsersByRoleName(RoleList.ROLE_ADMIN);
        List<User> warehouses = persistenceMethod.getUsersByRoleName(RoleList.ROLE_WAREHOUSE);
        auditors.forEach(user -> notificationService.createNotification("Exit registered", message, user.getId()));
        admins.forEach(user -> notificationService.createNotification("Exit registered", message, user.getId()));
        warehouses.forEach(user -> notificationService.createNotification("Exit registered", message, user.getId()));

        recordMetrics(sample, "registerInventoryExit");
        log.info("(registerInventoryExit) -> " + OPERATION_COMPLETED);
        return ApiResponse.success("Inventory exit registered successfully", null);
    }

    @Override
    @Transactional
    @CacheEvict(value = {INVENTORY_BY_ID, ALL_ACTIVE_INVENTORIES, ALL_DELETED_INVENTORIES}, allEntries = true)
    public ApiResponse<Void> registerInventoryExitForDisposal(ExitDisposalDto exitDisposalDto) {
        Inventory inventory = persistenceMethod.getInventoryById(exitDisposalDto.getInventoryId());
        if (Boolean.FALSE.equals(inventory.getActive())) {
            throw new IllegalArgumentException("Inventory is inactive");
        }
        if (exitDisposalDto.getQuantity() == null || exitDisposalDto.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        BigDecimal available = inventory.getAvailableQuantity() == null ? BigDecimal.ZERO : inventory.getAvailableQuantity();
        if (available.compareTo(exitDisposalDto.getQuantity()) < 0) {
            throw new IllegalArgumentException("Insufficient stock for disposal");
        }

        inventory.setAvailableQuantity(available.subtract(exitDisposalDto.getQuantity()));
        inventory.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        inventory.setUpdatedBy(persistenceMethod.getCurrentUserEmail());
        inventoryRepository.save(inventory);

        NewMovementDto movementDto = NewMovementDto.builder()
                .type(MovementType.EXIT)
                .inventoryId(inventory.getId())
                .productId(inventory.getProduct().getId())
                .quantity(exitDisposalDto.getQuantity())
                .motive(exitDisposalDto.getMotive() != null ? exitDisposalDto.getMotive() : "Expired/Damaged product disposal")
                .build();
        movementService.createMovement(movementDto);

        return ApiResponse.success("Exit registered for disposal", null);
    }

    @Override
    @Transactional
    @CacheEvict(value = {INVENTORY_BY_ID, ALL_ACTIVE_INVENTORIES, ALL_DELETED_INVENTORIES}, allEntries = true)
    public ApiResponse<Void> registerInventoryTransfer(ExitTransferDto exitTransferDto) {
        if (exitTransferDto.getQuantity() == null || exitTransferDto.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        Inventory origin = persistenceMethod.getInventoryById(exitTransferDto.getOriginInventoryId());
        Inventory destination = persistenceMethod.getInventoryById(exitTransferDto.getDestinationInventoryId());

        if (Boolean.FALSE.equals(origin.getActive()) || Boolean.FALSE.equals(destination.getActive())) {
            throw new IllegalArgumentException("Origin or destination inventory is inactive");
        }

        BigDecimal availableOrigin = origin.getAvailableQuantity() == null ? BigDecimal.ZERO : origin.getAvailableQuantity();
        if (availableOrigin.compareTo(exitTransferDto.getQuantity()) < 0) {
            throw new IllegalArgumentException("Insufficient stock in origin warehouse");
        }

        // EXIT en origen
        origin.setAvailableQuantity(availableOrigin.subtract(exitTransferDto.getQuantity()));
        origin.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        origin.setUpdatedBy(persistenceMethod.getCurrentUserEmail());
        inventoryRepository.save(origin);

        NewMovementDto exitMovement = NewMovementDto.builder()
                .type(MovementType.EXIT)
                .inventoryId(origin.getId())
                .productId(origin.getProduct().getId())
                .quantity(exitTransferDto.getQuantity())
                .motive(exitTransferDto.getMotive() != null ? exitTransferDto.getMotive() : "Warehouse transfer to " + destination.getWarehouse().getName())
                .build();
        movementService.createMovement(exitMovement);

        // ENTRY en destino
        BigDecimal availableDest = destination.getAvailableQuantity() == null ? BigDecimal.ZERO : destination.getAvailableQuantity();
        destination.setAvailableQuantity(availableDest.add(exitTransferDto.getQuantity()));
        destination.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        destination.setUpdatedBy(persistenceMethod.getCurrentUserEmail());
        inventoryRepository.save(destination);

        NewMovementDto entryMovement = NewMovementDto.builder()
                .type(MovementType.ENTRY)
                .inventoryId(destination.getId())
                .productId(destination.getProduct().getId())
                .quantity(exitTransferDto.getQuantity())
                .motive(exitMovement.getMotive() != null ? exitTransferDto.getMotive() : "Warehouse transfer from " + origin.getWarehouse().getName())
                .build();
        movementService.createMovement(entryMovement);

        return ApiResponse.success("Inventory transfer completed", null);
    }

    @Override
    @Transactional
    @CacheEvict(value = {INVENTORY_BY_ID, INVENTORY_BY_PRODUCT_WAREHOUSE, INVENTORIES_BY_PRODUCT, INVENTORIES_BY_WAREHOUSE, INVENTORIES_BY_LOW_STOCK, INVENTORIES_BY_AVAILABLE_PRODUCT, ALL_ACTIVE_INVENTORIES, ALL_DELETED_INVENTORIES, INVENTORIES_DELETED_BY_NAME}, allEntries = true)
    public ApiResponse<Void> adjustInventoryStock(UUID inventoryId,
                                                  BigDecimal newAvailableQuantity,
                                                  BigDecimal newReservedQuantity,
                                                  UUID userId,
                                                  String motive) {
        log.debug("(adjustInventoryStock) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);

        if (newAvailableQuantity.compareTo(BigDecimal.ZERO) < 0 || newReservedQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quantities cannot be negative");
        }
        if (newReservedQuantity.compareTo(newAvailableQuantity) > 0) {
            throw new IllegalArgumentException("Reserved quantity cannot exceed available quantity");
        }

        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new EntityNotFoundException("Inventory not found"));

        if (Boolean.FALSE.equals(inventory.getActive())) {
            throw new IllegalArgumentException(ENTITY_INACTIVE.formatted(INVENTORY_ID.formatted(inventoryId)));
        }

        final int MAX_RETRIES = 3;
        int attempt = 0;

        while (true) {
            try {
                inventory.setAvailableQuantity(newAvailableQuantity);
                inventory.setReservedQuantity(newReservedQuantity);
                inventory.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
                inventory.setUpdatedBy(persistenceMethod.getCurrentUserEmail());
                inventoryRepository.save(inventory);
                NewMovementDto movementDto = NewMovementDto.builder()
                        .type(MovementType.ADJUSTMENT)
                        .inventoryId(inventory.getId())
                        .productId(inventory.getProduct().getId())
                        .quantity(newAvailableQuantity)
                        .motive(motive)
                        .build();
                movementService.createMovement(movementDto);
                String message = "The stock for product " + inventory.getProduct().getName() +
                        " has been adjusted in warehouse " + inventory.getWarehouse().getName() + ".";
                List<User> admins = persistenceMethod.getUsersByRoleName(RoleList.ROLE_ADMIN);
                List<User> auditors = persistenceMethod.getUsersByRoleName(RoleList.ROLE_AUDITOR);
                auditors.forEach(u -> notificationService.createNotification("Stock adjusted", message, u.getId()));
                admins.forEach(u -> notificationService.createNotification("Stock adjusted", message, u.getId()));
                recordMetrics(sample, "adjustInventoryStock");
                log.info("(adjustInventoryStock) -> " + OPERATION_COMPLETED);
                return ApiResponse.success(OPERATION_COMPLETED_SUCCESSFULLY.formatted("adjust Stock"), null);
            } catch (org.springframework.dao.OptimisticLockingFailureException ex) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    log.error("(adjustInventoryStock) -> Optimistic lock failure after {} attempts for inventory {}", attempt, inventoryId, ex);
                    throw ex;
                }
                inventory = inventoryRepository.findById(inventoryId)
                        .orElseThrow(() -> new EntityNotFoundException("Inventory not found during retry"));
            } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                log.warn("(adjustInventoryStock) -> Data integrity violation for inventory {}", inventoryId, ex);
                throw new IllegalArgumentException("Unable to adjust inventory due to data integrity constraints");
            }
        }
    }

    // utility method for other services, not exposed in controllers.
    @Override
    @Transactional(readOnly = true)
    public ApiResponse<Boolean> existsInventory(UUID productId, UUID warehouseId, String lot) {
        log.debug("(existsInventory) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Boolean exists = inventoryRepository.existsByProductIdAndWarehouseIdAndLotAndActiveTrue(productId, warehouseId, lot);
        recordMetrics(sample, "existsInventory");
        log.info("(existsInventory) -> " + OPERATION_COMPLETED);
        if (Boolean.FALSE.equals(exists)) {
            return ApiResponse.success(ENTITY_NOT_EXISTING.formatted(INVENTORY), false);
        }
        return ApiResponse.success(ENTITY_EXISTING.formatted(INVENTORY), true);
    }

    @Override
    @Transactional
    public ApiResponse<Void> reserveInventoryStock(UUID inventoryId,
                                                   BigDecimal quantity,
                                                   UUID orderId,
                                                   UUID userId,
                                                   String motive) {
        log.debug("(reserveInventoryStock) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        Inventory inventory = persistenceMethod.getInventoryById(inventoryId);
        if (Boolean.FALSE.equals(inventory.getActive())) {
            throw new IllegalArgumentException(ENTITY_INACTIVE.formatted(INVENTORY_ID.formatted(inventoryId)));
        }
        Order order = null;
        if (orderId != null) {
            order = persistenceMethod.getOrderById(orderId);
        }
        final int MAX_RETRIES = 3;
        int attempt = 0;
        while (true) {
            try {
                BigDecimal available = inventory.getAvailableQuantity() == null ? BigDecimal.ZERO : inventory.getAvailableQuantity();
                BigDecimal reserved = inventory.getReservedQuantity() == null ? BigDecimal.ZERO : inventory.getReservedQuantity();
                BigDecimal availableForReservation = available.subtract(reserved);
                if (availableForReservation.compareTo(quantity) < 0) {
                    throw new IllegalArgumentException("Not enough available stock to reserve");
                }
                inventory.setReservedQuantity(reserved.add(quantity));
                inventory.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
                inventory.setUpdatedBy(persistenceMethod.getCurrentUserEmail());
                inventoryRepository.save(inventory);
                NewMovementDto movementDto = NewMovementDto.builder()
                        .type(MovementType.RESERVE)
                        .inventoryId(inventory.getId())
                        .productId(inventory.getProduct().getId())
                        .quantity(quantity)
                        .orderId(orderId)
                        .motive(motive)
                        .dispatcherId(null)
                        .build();
                movementService.createMovement(movementDto);
                String clientMessage = "A stock of " + quantity + " units of product " + inventory.getProduct().getName() +
                        " has been reserved in warehouse " + inventory.getWarehouse().getName() + " (Lot: " + inventory.getLot() + ").";
                if (order != null && order.getClient() != null) {
                    notificationService.createNotification(
                            "Stock reserved for your order",
                            clientMessage,
                            order.getClient().getId()
                    );
                }
                if (order != null && order.getUser() != null) {
                    notificationService.createNotification(
                            "Stock reserved for order",
                            clientMessage + " Order ID: " + order.getId(),
                            order.getUser().getId()
                    );
                }
                List<User> admins = persistenceMethod.getUsersByRoleName(RoleList.ROLE_ADMIN);
                List<User> warehouses = persistenceMethod.getUsersByRoleName(RoleList.ROLE_WAREHOUSE);
                admins.forEach(u -> notificationService.createNotification("Stock reserved", clientMessage, u.getId()));
                warehouses.forEach(u -> notificationService.createNotification("Stock reserved", clientMessage, u.getId()));
                recordMetrics(sample, "reserveInventoryStock");
                log.info("(reserveInventoryStock) -> " + OPERATION_COMPLETED);
                return ApiResponse.success("Stock reserved", null);
            } catch (org.springframework.dao.OptimisticLockingFailureException ex) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    log.error("(reserveInventoryStock) -> Optimistic lock failure after {} attempts for inventory {}", attempt, inventoryId, ex);
                    throw ex;
                }
                inventory = inventoryRepository.findById(inventoryId)
                        .orElseThrow(() -> new IllegalArgumentException("Inventory not found during retry"));
            }
        }
    }

    @Override
    @Transactional
    public ApiResponse<Void> releaseInventoryReservation(UUID inventoryId, BigDecimal quantity, UUID orderId, UUID userId, String motive) {
        log.debug("(releaseInventoryReservation) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new EntityNotFoundException("Inventory not found"));
        BigDecimal reserved = inventory.getReservedQuantity();
        if (reserved.compareTo(quantity) < 0) {
            quantity = reserved;
        }
        inventory.setReservedQuantity(reserved.subtract(quantity));
        inventory.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        inventory.setUpdatedBy(persistenceMethod.getCurrentUserEmail());
        inventoryRepository.save(inventory);
        movementService.createMovement(NewMovementDto.builder()
                .type(MovementType.ADJUSTMENT)
                .inventoryId(inventory.getId())
                .productId(inventory.getProduct().getId())
                .quantity(quantity)
                .orderId(orderId)
                .motive(motive)
                .build());
        BigDecimal finalQuantity = quantity;
        List<User> admins = persistenceMethod.getUsersByRoleName(RoleList.ROLE_ADMIN);
        List<User> auditors = persistenceMethod.getUsersByRoleName(RoleList.ROLE_AUDITOR);
        auditors.forEach(user ->
                notificationService.createNotification(
                        "Reservation released",
                        "A reservation of " + finalQuantity + " units of product " + inventory.getProduct().getName() +
                                " has been released in warehouse " + inventory.getWarehouse().getName() + ".",
                        user.getId()
                ));
        admins.forEach(user ->
                notificationService.createNotification(
                        "Reservation released",
                        "A reservation of " + finalQuantity + " units of product " + inventory.getProduct().getName() +
                                " has been released in warehouse " + inventory.getWarehouse().getName() + ".",
                        user.getId()
                ));
        recordMetrics(sample, "releaseInventoryReservation");
        log.info("(releaseInventoryReservation) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(OPERATION_COMPLETED_SUCCESSFULLY.formatted("release Reservation"), null);
    }

    private void recordMetrics(Timer.Sample sample, String operation) {
        sample.stop(Timer.builder("inventory.service.latency")
                .tag("operation", operation)
                .register(meterRegistry));
        meterRegistry.counter("inventory.service.operations", "type", operation).increment();
    }
}
