package com.sigi.services.service.warehouse;

import com.sigi.persistence.entity.Order;
import com.sigi.persistence.entity.Warehouse;
import com.sigi.persistence.repository.InventoryRepository;
import com.sigi.persistence.repository.WarehouseRepository;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.presentation.dto.warehouse.NewWarehouseDto;
import com.sigi.presentation.dto.warehouse.WarehouseDto;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.sigi.util.Constants.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;
    private final DtoMapper dtoMapper;
    private final MeterRegistry meterRegistry;
    private final PersistenceMethod persistenceMethod;

    @Override
    @Transactional
    @CacheEvict(value = {WAREHOUSE_BY_ID, WAREHOUSES_ACTIVE, WAREHOUSES_BY_CAPACITY, ALL_DELETED_WAREHOUSES, WAREHOUSES_DELETED_BY_NAME, WAREHOUSE_NAME,}, allEntries = true)
    public ApiResponse<WarehouseDto> createWarehouse(NewWarehouseDto dto) {
        log.debug("(createWarehouse) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        if (warehouseRepository.existsByName(dto.getName())) {
            Warehouse existingWarehouse = persistenceMethod.getWarehouseByName(dto.getName());
            if (Boolean.TRUE.equals(existingWarehouse.getActive())) {
                throw new IllegalArgumentException(ENTITY_ALREADY_EXISTS.formatted(WAREHOUSE_NAME.formatted(dto.getName())));
            } else {
                throw new IllegalArgumentException(ENTITY_INACTIVE.formatted(WAREHOUSE_NAME.formatted(dto.getName())));
            }
        }
        Warehouse warehouse = Warehouse.builder()
                .name(dto.getName())
                .location(dto.getLocation())
                .totalCapacity(dto.getTotalCapacity() == null ? 0 : dto.getTotalCapacity())
                .active(true)
                .createdAt(LocalDateTime.now(ZoneId.of("America/Bogota")))
                .createdBy(persistenceMethod.getCurrentUserEmail())
                .build();
        Warehouse saved = warehouseRepository.save(warehouse);
        recordMetrics(sample, "createWarehouse");
        log.info("(createWarehouse) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(CREATED_SUCCESSFULLY.formatted(WAREHOUSE),
                dtoMapper.toWarehouseDto(saved));
    }

    @Override
    @Transactional
    @CacheEvict(value = {WAREHOUSE_BY_ID, WAREHOUSES_ACTIVE, WAREHOUSES_BY_CAPACITY, ALL_DELETED_WAREHOUSES, WAREHOUSES_DELETED_BY_NAME, WAREHOUSE_NAME,}, allEntries = true)
    public ApiResponse<WarehouseDto> updateWarehouse(UUID id, NewWarehouseDto dto) {
        log.debug("(updateWarehouse) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Warehouse existing = persistenceMethod.getWarehouseById(id);
        if (existing.getActive().equals(Boolean.FALSE)) {
            throw new IllegalArgumentException(ENTITY_INACTIVE.formatted(WAREHOUSE_ID.formatted(id)));
        }
        existing.setName(dto.getName());
        existing.setLocation(dto.getLocation());
        existing.setTotalCapacity(dto.getTotalCapacity() == null ? existing.getTotalCapacity() : dto.getTotalCapacity());
        existing.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        existing.setUpdatedBy(persistenceMethod.getCurrentUserEmail());
        Warehouse updated = warehouseRepository.save(existing);
        recordMetrics(sample, "updateWarehouse");
        log.info("(updateWarehouse) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(UPDATED_SUCCESSFULLY.formatted(WAREHOUSE),
                dtoMapper.toWarehouseDto(updated));
    }

    @Override
    @Transactional
    @CacheEvict(value = {WAREHOUSE_BY_ID, WAREHOUSES_ACTIVE, WAREHOUSES_BY_CAPACITY, ALL_DELETED_WAREHOUSES, WAREHOUSES_DELETED_BY_NAME, WAREHOUSE_NAME,}, allEntries = true)
    public ApiResponse<Void> deleteWarehouse(UUID id) {
        log.debug("(deleteWarehouse) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Warehouse existing = persistenceMethod.getWarehouseById(id);
        if (existing.getActive().equals(Boolean.FALSE)) {
            throw new IllegalArgumentException(ENTITY_PREVIOUSLY_DELETED.formatted(WAREHOUSE_ID.formatted(id)));
        }
        boolean hasInventory = inventoryRepository.existsByWarehouseIdAndActiveTrue(id);
        if (hasInventory) {
            throw new IllegalStateException("Cannot delete warehouse with active inventory");
        }
        existing.setActive(false);
        existing.setDeletedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        existing.setDeletedBy(persistenceMethod.getCurrentUserEmail());
        warehouseRepository.save(existing);
        recordMetrics(sample, "deleteWarehouse");
        log.info("(deleteWarehouse) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(DELETED_SUCCESSFULLY.formatted(WAREHOUSE), null);
    }

    @Override
    @Transactional
    @CacheEvict(value = {WAREHOUSE_BY_ID, WAREHOUSES_ACTIVE, WAREHOUSES_BY_CAPACITY, ALL_DELETED_WAREHOUSES, WAREHOUSES_DELETED_BY_NAME, WAREHOUSE_NAME,}, allEntries = true)
    public ApiResponse<Void> restoreWarehouse(UUID id) {
        log.debug("(restoreWarehouse) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Warehouse existing = persistenceMethod.getWarehouseById(id);
        if (Boolean.TRUE.equals(existing.getActive())) {
            throw new IllegalArgumentException(ENTITY_ALREADY_ACTIVE.formatted(WAREHOUSE_ID.formatted(id)));
        }
        if (warehouseRepository.existsByNameAndActiveTrue(existing.getName())) {
            throw new IllegalStateException("Cannot restore warehouse: another active warehouse with same name exists");
        }
        BigDecimal used = inventoryRepository.sumUsedCapacityByWarehouseId(existing.getId()).orElse(BigDecimal.ZERO);
        if (BigDecimal.valueOf(existing.getTotalCapacity()).compareTo(used) < 0) {
            throw new IllegalStateException("Cannot restore warehouse: used capacity exceeds total capacity");
        }
        existing.setActive(true);
        existing.setDeletedAt(null);
        existing.setDeletedBy(null);
        existing.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        existing.setUpdatedBy(persistenceMethod.getCurrentUserEmail());
        warehouseRepository.save(existing);
        recordMetrics(sample, "restoreWarehouse");
        log.info("(restoreWarehouse) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(ENTITY_ACTIVATED_SUCCESSFULLY.formatted(WAREHOUSE_ID.formatted(id)), null);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = WAREHOUSE_BY_ID, key = "#id")
    public ApiResponse<WarehouseDto> getWarehouseById(UUID id) {
        log.debug("(getWarehouseById) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Warehouse response = persistenceMethod.getWarehouseById(id);
        if (Boolean.FALSE.equals(response.getActive())) {
            throw new IllegalArgumentException(ENTITY_INACTIVE.formatted(WAREHOUSE_ID.formatted(id)));
        }
        recordMetrics(sample, "getWarehouseById");
        log.info("(getWarehouseById) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(SEARCH_SUCCESSFULLY.formatted(WAREHOUSE),
                dtoMapper.toWarehouseDto(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = WAREHOUSE_BY_ID, key = "#id")
    public ApiResponse<WarehouseDto> getDeletedWarehouseById(UUID id) {
        log.debug("(getDeletedWarehouseById) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Warehouse response = warehouseRepository.findByIdAndActiveFalse(id).orElseThrow(() -> new EntityNotFoundException(ENTITY_NOT_FOUND.formatted(WAREHOUSE_ID.formatted(id))));
        recordMetrics(sample, "getDeletedWarehouseById");
        log.info("(getDeletedWarehouseById) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(SEARCH_SUCCESSFULLY.formatted(ORDER),
                dtoMapper.toWarehouseDto(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = WAREHOUSES_ACTIVE,  key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<WarehouseDto>> listAllActiveWarehouse(PagedRequestDto pagedRequestDto) {
        log.debug("(listAllActiveWarehouse) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Warehouse> response = warehouseRepository.findByActiveTrue(PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        recordMetrics(sample, "listAllActiveWarehouse");
        log.info("(listAllActiveWarehouse) -> " + OPERATION_COMPLETED);
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(WAREHOUSES),
                dtoMapper.toWarehouseDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ALL_DELETED_WAREHOUSES,  key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<WarehouseDto>> listAllDeletedWarehouse(PagedRequestDto pagedRequestDto) {
        log.debug("(listAllDeletedWarehouse) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Warehouse> response = warehouseRepository.findByActiveFalse(PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        recordMetrics(sample, "listAllDeletedWarehouse");
        log.info("(listAllDeletedWarehouse) -> " + OPERATION_COMPLETED);
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(SEARCH_SUCCESSFULLY.formatted("deleted warehouses")),
                dtoMapper.toWarehouseDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = WAREHOUSES_BY_CAPACITY, key = "{#capacity,#pagedRequestDto}")
    public ApiResponse<PagedResponse<WarehouseDto>> listWarehouseByCapacityGreaterOrEqual(Integer capacity, PagedRequestDto pagedRequestDto) {
        log.debug("(listWarehouseByCapacityGreaterOrEqual) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Warehouse> response = warehouseRepository.findByGreaterOrEqualCapacityAndActiveTrue(capacity, PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        recordMetrics(sample, "listWarehouseByCapacityGreaterOrEqual");
        log.info("(listWarehouseByCapacityGreaterOrEqual) -> " + OPERATION_COMPLETED);
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(SEARCH_SUCCESSFULLY.formatted(WAREHOUSES)),
                dtoMapper.toWarehouseDtoPage(response));
    }


    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = WAREHOUSE_NAME, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<WarehouseDto>> listWarehouseByName(PagedRequestDto pagedRequestDto) {
        log.debug("(listWarehouseByName) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Warehouse> response = warehouseRepository.findByNameContainingIgnoreCaseAndActiveTrue(pagedRequestDto.getSearchValue(), PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        recordMetrics(sample, "listWarehouseByName");
        log.info("(listWarehouseByName) -> " + OPERATION_COMPLETED);
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(SEARCH_SUCCESSFULLY.formatted("warehouses with name containing '%s'".formatted(pagedRequestDto.getSearchValue()))),
                dtoMapper.toWarehouseDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = WAREHOUSES_DELETED_BY_NAME, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<WarehouseDto>> listDeletedWarehouseByName(PagedRequestDto pagedRequestDto) {
        log.debug("(listDeletedWarehouseByName) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Warehouse> response = warehouseRepository.findByNameContainingIgnoreCaseAndActiveFalse(pagedRequestDto.getSearchValue(), PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        recordMetrics(sample, "listDeletedWarehouseByName");
        log.info("(listDeletedWarehouseByName) -> " + OPERATION_COMPLETED);
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(SEARCH_SUCCESSFULLY.formatted("deleted warehouses with name containing '%s'".formatted(pagedRequestDto.getSearchValue()))),
                dtoMapper.toWarehouseDtoPage(response));
    }

    // Utility method to record metrics for each operation and not expose it to controllers, only for internal use in the service logic
    @Override
    @Transactional
    @CacheEvict(value = {WAREHOUSE_BY_ID, WAREHOUSES_ACTIVE, WAREHOUSES_BY_CAPACITY, ALL_DELETED_WAREHOUSES, WAREHOUSES_DELETED_BY_NAME, WAREHOUSE_NAME,}, allEntries = true)
    public ApiResponse<Boolean> validateAvailableCapacity(UUID warehouseId, BigDecimal incomingQuantity) {
        log.debug("(validateAvailableCapacity) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Warehouse w = warehouseRepository.findById(warehouseId).orElseThrow(() -> new EntityNotFoundException("Warehouse not found"));
        BigDecimal used = inventoryRepository.sumUsedCapacityByWarehouseId(warehouseId).orElse(BigDecimal.ZERO);
        BigDecimal available = BigDecimal.valueOf(w.getTotalCapacity()).subtract(used);
        boolean ok = available.compareTo(incomingQuantity == null ? BigDecimal.ZERO : incomingQuantity) >= 0;
        recordMetrics(sample, "validateAvailableCapacity");
        log.info("(validateAvailableCapacity) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(OPERATION_COMPLETED_SUCCESSFULLY.formatted("Capacity validation"), ok);
    }

    private void recordMetrics(Timer.Sample sample, String operation) {
        sample.stop(Timer.builder("warehouse.service.latency")
                .tag("operation", operation)
                .register(meterRegistry));
        meterRegistry.counter("warehouse.service.operations", "type", operation).increment();
    }

}
