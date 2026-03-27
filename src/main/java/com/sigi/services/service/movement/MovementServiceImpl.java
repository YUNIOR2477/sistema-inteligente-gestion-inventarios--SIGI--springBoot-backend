package com.sigi.services.service.movement;

import com.sigi.persistence.entity.*;
import com.sigi.persistence.enums.MovementType;
import com.sigi.persistence.enums.RoleList;
import com.sigi.persistence.repository.*;
import com.sigi.presentation.dto.movement.MovementDto;
import com.sigi.presentation.dto.movement.NewMovementDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static com.sigi.util.Constants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovementServiceImpl implements MovementService {
    private final MovementRepository movementRepository;
    private final DtoMapper dtoMapper;
    private final MeterRegistry meterRegistry;
    private final PersistenceMethod persistenceMethod;
    private final NotificationService notificationService;

    @Override
    @Transactional
    @CacheEvict(value = {MOVEMENTS_BY_PRODUCT, MOVEMENTS_BY_ORDER, MOVEMENTS_BY_DISPATCHER, MOVEMENTS_BY_TYPE, ALL_ACTIVE_MOVEMENTS, ALL_DELETED_MOVEMENTS, MOVEMENTS_DELETED_BY_TYPE, MOVEMENT_BY_ID}, allEntries = true)
    public ApiResponse<MovementDto> updateMovement(UUID id, NewMovementDto dto) {
        log.debug("(updateMovement) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Movement movement = persistenceMethod.getMovementById(id);
        if (Boolean.FALSE.equals(movement.getActive())) {
            throw new IllegalArgumentException(ENTITY_INACTIVE.formatted(MOVEMENT_ID.formatted(id)));
        }

        if (dto.getType() == MovementType.EXIT) {
            if (dto.getDispatcherId() == null) {
                throw new IllegalArgumentException("Dispatcher cannot be null for EXIT movements");
            }
            if (dto.getOrderId() == null) {
                throw new IllegalArgumentException("Order cannot be null for EXIT movements");
            }
        }
        User user = persistenceMethod.getCurrentUser();
        Inventory inventory = null;
        if (dto.getInventoryId() != null) {
            inventory = persistenceMethod.getInventoryById(dto.getInventoryId());
            if (Boolean.FALSE.equals(inventory.getActive())) {
                throw new IllegalArgumentException("Inventory is inactive");
            }
        }
        Product product = null;
        if (inventory != null) {
            product = inventory.getProduct();
        } else {
            if (dto.getProductId() == null) {
                throw new IllegalArgumentException("ProductId is required when inventoryId is not provided");
            }
            product = persistenceMethod.getProductById(dto.getProductId());
        }
        if (inventory != null && dto.getProductId() != null) {
            if (!inventory.getProduct().getId().equals(dto.getProductId())) {
                throw new IllegalArgumentException("ProductId does not match inventory.productId");
            }
        }
        Order order = null;
        if (dto.getOrderId() != null) {
            order = persistenceMethod.getOrderById(dto.getOrderId());
        }
        Dispatcher dispatcher = null;
        if (dto.getDispatcherId() != null) {
            dispatcher = persistenceMethod.getDispatcherById(dto.getDispatcherId());
        }
        if (movement.getInventory() != null && dto.getInventoryId() != null
                && !movement.getInventory().getId().equals(dto.getInventoryId())) {
            log.warn("(updateMovement) -> Changing inventory on existing movement id: {} from {} to {}",
                    id, movement.getInventory().getId(), dto.getInventoryId());
        }
        movement.setType(dto.getType());
        movement.setInventory(inventory); // puede ser null
        movement.setProduct(product);
        movement.setQuantity(dto.getQuantity());
        movement.setUser(user);
        movement.setOrder(order);
        movement.setDispatcher(dispatcher);
        movement.setMotive(dto.getMotive());
        movement.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        movement.setUpdatedBy(persistenceMethod.getCurrentUserEmail());
        Movement updated;
        try {
            updated = movementRepository.save(movement);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            log.warn("(updateMovement) -> DataIntegrityViolation while updating movement id: {}", id, ex);
            throw new IllegalArgumentException("Unable to update movement due to data integrity constraints");
        }
        String notificationMessage = "A movement has been updated in the system id: %s".formatted(updated.getId());
        List<User> dispatchers = persistenceMethod.getUsersByRoleName(RoleList.ROLE_DISPATCHER);
        List<User> admins = persistenceMethod.getUsersByRoleName(RoleList.ROLE_ADMIN);
        List<User> warehouses = persistenceMethod.getUsersByRoleName(RoleList.ROLE_WAREHOUSE);
        dispatchers.forEach(u -> notificationService.createNotification("Movement Updated", notificationMessage, u.getId()));
        warehouses.forEach(u -> notificationService.createNotification("Movement Updated", notificationMessage, u.getId()));
        admins.forEach(u -> notificationService.createNotification("Movement Updated", notificationMessage, u.getId()));
        recordMetrics(sample, "updateMovement");
        log.info("(updateMovement) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(UPDATED_SUCCESSFULLY.formatted(MOVEMENT), dtoMapper.toMovementDto(updated));
    }

    @Override
    @Transactional
    @CacheEvict(value = {MOVEMENTS_BY_PRODUCT, MOVEMENTS_BY_ORDER, MOVEMENTS_BY_DISPATCHER, MOVEMENTS_BY_TYPE, ALL_ACTIVE_MOVEMENTS, ALL_DELETED_MOVEMENTS, MOVEMENTS_DELETED_BY_TYPE, MOVEMENT_BY_ID}, allEntries = true)
    public ApiResponse<Void> deleteMovement(UUID id) {
        log.debug("(deleteMovement) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Movement movement = persistenceMethod.getMovementById(id);
        if (movement.getActive().equals(Boolean.FALSE)) {
            throw new IllegalArgumentException(ENTITY_INACTIVE.formatted(MOVEMENT_ID.formatted(id)));
        }
        movement.setActive(false);
        movement.setDeletedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        movement.setDeletedBy(persistenceMethod.getCurrentUserEmail());
        movementRepository.save(movement);
        log.info("(deleteMovement) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "deleteMovement");
        return ApiResponse.success(DELETED_SUCCESSFULLY.formatted(MOVEMENT), null);
    }

    @Override
    @Transactional
    @CacheEvict(value = {MOVEMENTS_BY_PRODUCT, MOVEMENTS_BY_ORDER, MOVEMENTS_BY_DISPATCHER, MOVEMENTS_BY_TYPE, ALL_ACTIVE_MOVEMENTS, ALL_DELETED_MOVEMENTS, MOVEMENTS_DELETED_BY_TYPE, MOVEMENT_BY_ID}, allEntries = true)
    public ApiResponse<Void> restoreMovement(UUID id) {
        log.debug("(restoreMovement) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Movement movement = persistenceMethod.getMovementById(id);
        if (movement.getActive().equals(Boolean.TRUE)) {
            throw new IllegalArgumentException(ENTITY_ALREADY_ACTIVE.formatted(MOVEMENT_ID.formatted(id)));
        }
        movement.setActive(true);
        movement.setDeletedAt(null);
        movement.setDeletedBy(null);
        movementRepository.save(movement);
        log.info("(restoreMovement) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "restoreMovement");
        return ApiResponse.success(ENTITY_ACTIVATED_SUCCESSFULLY.formatted(MOVEMENT), null);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = MOVEMENT_BY_ID, key = "#id")
    public ApiResponse<MovementDto> getMovementById(UUID id) {
        log.debug("(getMovementById) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Movement movement = persistenceMethod.getMovementById(id);
        if (movement.getActive().equals(Boolean.FALSE)) {
            throw new IllegalArgumentException(ENTITY_INACTIVE.formatted(MOVEMENT_ID.formatted(id)));
        }
        log.info("(getMovementById) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "getMovementById");
        return ApiResponse.success(SEARCH_SUCCESSFULLY.formatted(MOVEMENT),
                dtoMapper.toMovementDto(movement));
    }

    @Override
    public ApiResponse<MovementDto> getDeletedMovementById(UUID id) {
        log.debug("(getDeletedMovementById) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Movement movement = movementRepository.findByIdAndActiveFalse(id).orElseThrow(() -> new EntityNotFoundException(ENTITY_NOT_FOUND.formatted(MOVEMENT_ID.formatted(id))));
        log.info("(getDeletedMovementById) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "getDeletedMovementById");
        return ApiResponse.success(SEARCH_SUCCESSFULLY.formatted(MOVEMENT),
                dtoMapper.toMovementDto(movement));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ALL_ACTIVE_MOVEMENTS, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<MovementDto>> listAllActiveMovements(PagedRequestDto pagedRequestDto) {
        log.debug("(listAllActiveMovements) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Movement> response = movementRepository.findByActiveTrue(PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        log.info("(listAllActiveMovements) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "listAllActiveMovements");
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(MOVEMENTS),
                dtoMapper.toMovementDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ALL_DELETED_MOVEMENTS, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<MovementDto>> listAllDeletedMovements(PagedRequestDto pagedRequestDto) {
        log.debug("(listAllDeletedMovements) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Movement> response = movementRepository.findByActiveFalse(PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        log.info("(listAllDeletedMovements) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "listAllDeletedMovements");
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(MOVEMENTS),
                dtoMapper.toMovementDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = MOVEMENTS_BY_PRODUCT, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<MovementDto>> listMovementsByProduct(PagedRequestDto pagedRequestDto) {
        log.debug("(listMovementsByProduct) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Movement> response = movementRepository.findByProductNameContainingIgnoreCaseAndActiveTrue(pagedRequestDto.getSearchValue(), PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        log.info("(listMovementsByProduct) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "listMovementsByProduct");
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(MOVEMENTS),
                dtoMapper.toMovementDtoPage(response));

    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = MOVEMENTS_BY_ORDER, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<MovementDto>> listMovementsByOrder(PagedRequestDto pagedRequestDto) {
        log.debug("(listMovementsByOrder) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Movement> response = movementRepository.findByOrderClientNameContainingIgnoreCaseAndActiveTrue(pagedRequestDto.getSearchValue(), PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        log.info("(listMovementsByOrder) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "listMovementsByOrder");
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(MOVEMENTS),
                dtoMapper.toMovementDtoPage(response));

    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = MOVEMENTS_BY_DISPATCHER, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<MovementDto>> listMovementsByDispatcher(PagedRequestDto pagedRequestDto) {
        log.debug("(listMovementsByDispatcher) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Movement> response = movementRepository.findByDispatcherNameContainingIgnoreCaseAndActiveTrue(pagedRequestDto.getSearchValue(), PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        log.info("(listMovementsByDispatcher) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "listMovementsByDispatcher");
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(MOVEMENTS),
                dtoMapper.toMovementDtoPage(response));

    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = MOVEMENTS_BY_TYPE, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<MovementDto>> listMovementsByType(PagedRequestDto pagedRequestDto) {
        log.debug("(listMovementsByType) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        MovementType type = MovementType.valueOf(pagedRequestDto.getSearchValue());
        Page<Movement> response = movementRepository.findByTypeAndActiveTrue(type, PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        log.info("(listMovementsByType) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "listMovementsByType");
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(MOVEMENTS),
                dtoMapper.toMovementDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = MOVEMENTS_DELETED_BY_TYPE, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<MovementDto>> listDeletedMovementsByType(PagedRequestDto pagedRequestDto) {
        log.debug("(listDeletedMovementsByType) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        MovementType movementType = MovementType.valueOf(pagedRequestDto.getSearchValue());
        Page<Movement> response = movementRepository.findByTypeAndActiveFalse(movementType, PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        log.info("(listDeletedMovementsByType) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "listDeletedMovementsByType");
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(MOVEMENTS),
                dtoMapper.toMovementDtoPage(response));
    }

    // utility methods in the logic of other services and not in the controllers
    @Override
    @Transactional
    @CacheEvict(value = {
            MOVEMENTS_BY_PRODUCT, MOVEMENTS_BY_ORDER, MOVEMENTS_BY_DISPATCHER, MOVEMENTS_BY_TYPE, ALL_ACTIVE_MOVEMENTS, ALL_DELETED_MOVEMENTS,
            MOVEMENTS_DELETED_BY_TYPE, MOVEMENT_BY_ID}, allEntries = true)
    public ApiResponse<MovementDto> createMovement(NewMovementDto dto) {
        log.debug("(createMovement) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        if (dto == null) {
            throw new IllegalArgumentException("Movement data cannot be null");
        }
        if (dto.getType() == null) {
            throw new IllegalArgumentException("Movement type is required");
        }
        if (dto.getQuantity() == null || dto.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        User user = persistenceMethod.getCurrentUser();
        Inventory inventory = null;
        if (dto.getInventoryId() != null) {
            inventory = persistenceMethod.getInventoryById(dto.getInventoryId());
            if (Boolean.FALSE.equals(inventory.getActive())) {
                throw new IllegalArgumentException("Inventory is inactive");
            }
        }
        Product product = null;
        if (inventory != null) {
            product = inventory.getProduct();
        } else {
            if (dto.getProductId() == null) {
                throw new IllegalArgumentException("ProductId is required when inventoryId is not provided");
            }
            product = persistenceMethod.getProductById(dto.getProductId());
        }
        if (inventory != null && dto.getProductId() != null) {
            if (!inventory.getProduct().getId().equals(dto.getProductId())) {
                throw new IllegalArgumentException("ProductId does not match inventory.productId");
            }
        }
        Order order = null;
        if (dto.getOrderId() != null) {
            order = persistenceMethod.getOrderById(dto.getOrderId());
        }
        Dispatcher dispatcher = null;
        if (dto.getDispatcherId() != null) {
            dispatcher = persistenceMethod.getDispatcherById(dto.getDispatcherId());
        }
        Movement movement = Movement.builder()
                .type(dto.getType())
                .inventory(inventory)
                .product(product)
                .quantity(dto.getQuantity())
                .user(user)
                .order(order)
                .dispatcher(dispatcher)
                .motive(dto.getMotive())
                .active(true)
                .createdAt(LocalDateTime.now(ZoneId.of("America/Bogota")))
                .createdBy(persistenceMethod.getCurrentUserEmail())
                .build();
        Movement saved = movementRepository.save(movement);
        String notificationMessage = "A new movement has been registered in the system id: %s".formatted(saved.getId());
        List<User> dispatchers = persistenceMethod.getUsersByRoleName(RoleList.ROLE_DISPATCHER);
        List<User> admins = persistenceMethod.getUsersByRoleName(RoleList.ROLE_ADMIN);
        List<User> warehouses = persistenceMethod.getUsersByRoleName(RoleList.ROLE_WAREHOUSE);
        dispatchers.forEach(u -> notificationService.createNotification("New Movement Registered", notificationMessage, u.getId()));
        warehouses.forEach(u -> notificationService.createNotification("New Movement Registered", notificationMessage, u.getId()));
        admins.forEach(u -> notificationService.createNotification("New Movement Registered", notificationMessage, u.getId()));
        recordMetrics(sample, "createMovement");
        log.info("(createMovement) -> " + OPERATION_COMPLETED);
        MovementDto resultDto = dtoMapper.toMovementDto(saved);
        return ApiResponse.success(CREATED_SUCCESSFULLY.formatted(MOVEMENT), resultDto);
    }


    private void recordMetrics(Timer.Sample sample, String operation) {
        sample.stop(Timer.builder("movement.service.latency")
                .tag("operation", operation)
                .register(meterRegistry));
        meterRegistry.counter("movement.service.operations", "type", operation).increment();
    }

}
