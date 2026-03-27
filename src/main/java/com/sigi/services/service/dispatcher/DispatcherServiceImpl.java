package com.sigi.services.service.dispatcher;

import com.sigi.persistence.entity.Dispatcher;
import com.sigi.persistence.entity.User;
import com.sigi.persistence.enums.RoleList;
import com.sigi.persistence.repository.DispatcherRepository;
import com.sigi.persistence.repository.MovementRepository;
import com.sigi.presentation.dto.dispatcher.DispatcherDto;
import com.sigi.presentation.dto.dispatcher.NewDispatcherDto;
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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.sigi.util.Constants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DispatcherServiceImpl implements DispatcherService {
    private final DispatcherRepository dispatcherRepository;
    private final MovementRepository movementRepository;
    private final PersistenceMethod persistenceMethod;
    private final DtoMapper dtoMapper;
    private final MeterRegistry meterRegistry;
    private final NotificationService notificationService;

    @Override
    @Transactional
    @CacheEvict(value = {DISPATCHER_BY_ID, DISPATCHER_BY_NAME, ALL_ACTIVE_DISPATCHERS, DISPATCHERS_DELETED_BY_NAME, ALL_DELETED_DISPATCHERS}, allEntries = true)
    public ApiResponse<DispatcherDto> createDispatcher(NewDispatcherDto dispatcherDto) {
        log.debug("(createDispatcher) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Dispatcher response = dispatcherRepository.save(Dispatcher.builder()
                .name(dispatcherDto.getName())
                .contact(dispatcherDto.getContact())
                .identification(dispatcherDto.getIdentification())
                .phone(dispatcherDto.getPhone())
                .location(dispatcherDto.getLocation())
                .email(dispatcherDto.getEmail())
                .active(true)
                .orderList(new ArrayList<>())
                .createdAt(LocalDateTime.now(ZoneId.of("America/Bogota")))
                .createdBy(persistenceMethod.getCurrentUserEmail())
                .build());
        List<User> admins = persistenceMethod.getUsersByRoleName(RoleList.ROLE_ADMIN);
        List<User> warehouses = persistenceMethod.getUsersByRoleName(RoleList.ROLE_WAREHOUSE);
        admins.forEach(admin -> notificationService.createNotification(
                "New Dispatcher Created",
                "Dispatcher %s has been created.".formatted(response.getName()),
                admin.getId()
        ));
        warehouses.forEach(warehouse -> notificationService.createNotification(
                "New Dispatcher Created",
                "Dispatcher %s has been created.".formatted(response.getName()),
                warehouse.getId()
        ));
        recordMetrics(sample, "createDispatcher");
        log.info("(createDispatcher) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(CREATED_SUCCESSFULLY.formatted(DISPATCHER),
                dtoMapper.toDispatcherDto(response));

    }

    @Override
    @Transactional
    @CacheEvict(value = {DISPATCHER_BY_ID, DISPATCHER_BY_NAME, ALL_ACTIVE_DISPATCHERS, DISPATCHERS_DELETED_BY_NAME, ALL_DELETED_DISPATCHERS}, allEntries = true)
    public ApiResponse<DispatcherDto> updateDispatcher(UUID id, NewDispatcherDto dispatcherDto) {
        log.debug("(updateDispatcher) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Dispatcher existingDispatcher = persistenceMethod.getDispatcherById(id);
        if (existingDispatcher.getActive().equals(Boolean.FALSE)) {
            throw new IllegalArgumentException(ENTITY_INACTIVE.formatted(DISPATCHER_ID.formatted(id)));
        }
        existingDispatcher.setName(dispatcherDto.getName());
        existingDispatcher.setContact(dispatcherDto.getContact());
        existingDispatcher.setIdentification(dispatcherDto.getIdentification());
        existingDispatcher.setPhone(dispatcherDto.getPhone());
        existingDispatcher.setLocation(dispatcherDto.getLocation());
        existingDispatcher.setEmail(dispatcherDto.getEmail());
        existingDispatcher.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        existingDispatcher.setUpdatedBy(persistenceMethod.getCurrentUserEmail());
        Dispatcher response = dispatcherRepository.save(existingDispatcher);
        List<User> admins = persistenceMethod.getUsersByRoleName(RoleList.ROLE_ADMIN);
        List<User> warehouses = persistenceMethod.getUsersByRoleName(RoleList.ROLE_WAREHOUSE);
        admins.forEach(admin -> notificationService.createNotification(
                "Dispatcher Updated",
                "Dispatcher %s has been updated.".formatted(response.getName()),
                admin.getId()
        ));
        warehouses.forEach(warehouse -> notificationService.createNotification(
                "Dispatcher Updated",
                "Dispatcher %s has been updated.".formatted(response.getName()),
                warehouse.getId()
        ));
        recordMetrics(sample, "updateDispatcher");
        log.info("(updateDispatcher) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(UPDATED_SUCCESSFULLY.formatted(DISPATCHER),
                dtoMapper.toDispatcherDto(response));

    }

    @Override
    @Transactional
    @CacheEvict(value = {DISPATCHER_BY_ID, DISPATCHER_BY_NAME, ALL_ACTIVE_DISPATCHERS, DISPATCHERS_DELETED_BY_NAME, ALL_DELETED_DISPATCHERS}, allEntries = true)
    public ApiResponse<Void> deleteDispatcher(UUID id) {
        log.debug("(deleteDispatcher) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Dispatcher existingDispatcher = persistenceMethod.getDispatcherById(id);
        if (existingDispatcher.getActive().equals(Boolean.FALSE)) {
            throw new IllegalArgumentException(ENTITY_PREVIOUSLY_DELETED.formatted(DISPATCHER_ID.formatted(id)));
        }

        boolean hasMovements = movementRepository.existsByDispatcherIdAndActiveTrue(id);
        if (hasMovements) {
            throw new IllegalStateException("Cannot delete dispatcher with active movements");
        }
        existingDispatcher.setActive(false);
        existingDispatcher.setDeletedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        existingDispatcher.setDeletedBy(persistenceMethod.getCurrentUserEmail());
        dispatcherRepository.save(existingDispatcher);
        List<User> admins = persistenceMethod.getUsersByRoleName(RoleList.ROLE_ADMIN);
        List<User> warehouses = persistenceMethod.getUsersByRoleName(RoleList.ROLE_WAREHOUSE);
        admins.forEach(admin -> notificationService.createNotification(
                "Dispatcher Deactivated",
                "Dispatcher %s has been deactivated.".formatted(existingDispatcher.getName()),
                admin.getId()
        ));
        warehouses.forEach(warehouse -> notificationService.createNotification(
                "Dispatcher Deactivated",
                "Dispatcher %s has been deactivated.".formatted(existingDispatcher.getName()),
                warehouse.getId()
        ));
        recordMetrics(sample, "deleteDispatcher");
        log.info("(deleteDispatcher) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(DELETED_SUCCESSFULLY.formatted(DISPATCHER), null);
    }

    @Override
    @Transactional
    @CacheEvict(value = {DISPATCHER_BY_ID, DISPATCHER_BY_NAME, ALL_ACTIVE_DISPATCHERS, DISPATCHERS_DELETED_BY_NAME, ALL_DELETED_DISPATCHERS}, allEntries = true)
    public ApiResponse<Void> restoreDispatcher(UUID id) {
        log.debug("(restoreDispatcher) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Dispatcher existingDispatcher = persistenceMethod.getDispatcherById(id);
        if (Boolean.TRUE.equals(existingDispatcher.getActive())) {
            throw new IllegalArgumentException(ENTITY_ALREADY_ACTIVE.formatted(DISPATCHER_ID.formatted(id)));
        }
        if (dispatcherRepository.existsByNameAndActiveTrue(existingDispatcher.getName())) {
            throw new IllegalStateException("Cannot restore dispatcher: name conflict");
        }
        existingDispatcher.setActive(true);
        existingDispatcher.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        existingDispatcher.setUpdatedBy(persistenceMethod.getCurrentUserEmail());
        existingDispatcher.setDeletedAt(null);
        existingDispatcher.setDeletedBy(null);
        dispatcherRepository.save(existingDispatcher);
        List<User> admins = persistenceMethod.getUsersByRoleName(RoleList.ROLE_ADMIN);
        List<User> warehouses = persistenceMethod.getUsersByRoleName(RoleList.ROLE_WAREHOUSE);
        admins.forEach(admin -> notificationService.createNotification(
                "Dispatcher Activated",
                "Dispatcher %s has been activated.".formatted(existingDispatcher.getName()),
                admin.getId()
        ));
        warehouses.forEach(warehouse -> notificationService.createNotification(
                "Dispatcher Activated",
                "Dispatcher %s has been activated.".formatted(existingDispatcher.getName()),
                warehouse.getId()
        ));
        recordMetrics(sample, "restoreDispatcher");
        log.info("(restoreDispatcher) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(ENTITY_ACTIVATED_SUCCESSFULLY.formatted(DISPATCHER_ID.formatted(id)), null);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = DISPATCHER_BY_ID, key = "#id")
    public ApiResponse<DispatcherDto> getDispatcherById(UUID id) {
        log.debug("(getDispatcherById) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Dispatcher response = persistenceMethod.getDispatcherById(id);
        if (response.getActive().equals(Boolean.FALSE)) {
            throw new IllegalArgumentException(ENTITY_INACTIVE.formatted(DISPATCHER_ID.formatted(id)));
        }
        recordMetrics(sample, "getDispatcherById");
        log.info("(getDispatcherById) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(SEARCH_SUCCESSFULLY.formatted(DISPATCHER),
                dtoMapper.toDispatcherDto(response));

    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = DISPATCHER_BY_ID, key = "#id")
    public ApiResponse<DispatcherDto> getDeletedDispatcherById(UUID id) {
        log.debug("(getDeletedDispatcherById) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Dispatcher response = dispatcherRepository.findByIdAndActiveFalse(id).orElseThrow(() -> new EntityNotFoundException(ENTITY_NOT_FOUND.formatted(DISPATCHER_ID.formatted(id))));
        recordMetrics(sample, "getDeletedDispatcherById");
        log.info("(getDeletedDispatcherById) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(SEARCH_SUCCESSFULLY.formatted(DISPATCHER),
                dtoMapper.toDispatcherDto(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ALL_ACTIVE_DISPATCHERS, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<DispatcherDto>> listAllActiveDispatchers(PagedRequestDto pagedRequestDto) {
        log.debug("(listAllActiveDispatchers) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Dispatcher> response = dispatcherRepository.findByActiveTrue(PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        log.info("(listAllActiveDispatchers) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "listAllActiveDispatchers");
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(DISPATCHERS),
                dtoMapper.toDispatcherDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ALL_DELETED_DISPATCHERS, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<DispatcherDto>> listAllDeletedDispatchers(PagedRequestDto pagedRequestDto) {
        log.debug("(listAllDeletedDispatchers) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Dispatcher> response = dispatcherRepository.findByActiveFalse(PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        log.info("(listAllDeletedDispatchers) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "listAllDeletedDispatchers");
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(DISPATCHERS),
                dtoMapper.toDispatcherDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CLIENTS_BY_NAME, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<DispatcherDto>> listDispatcherByName(PagedRequestDto pagedRequestDto) {
        log.debug("(listDispatcherByName) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Dispatcher> response = dispatcherRepository.findByNameContainingIgnoreCaseAndActiveTrue(pagedRequestDto.getSearchValue(), PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        log.info("(listDispatcherByName) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "listDispatcherByName");
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(DISPATCHERS),
                dtoMapper.toDispatcherDtoPage(response));

    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = DISPATCHERS_DELETED_BY_NAME, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<DispatcherDto>> listDeletedDispatcherByName(PagedRequestDto pagedRequestDto) {
        log.debug("(listDeletedDispatcherByName) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Dispatcher> response = dispatcherRepository.findByNameContainingIgnoreCaseAndActiveFalse(pagedRequestDto.getSearchValue(), PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        log.info("(listDeletedDispatcherByName) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "listDeletedDispatcherByName");
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(DISPATCHERS),
                dtoMapper.toDispatcherDtoPage(response));
    }

    private void recordMetrics(Timer.Sample sample, String operation) {
        sample.stop(Timer.builder("dispatcher.service.latency")
                .tag("operation", operation)
                .register(meterRegistry));
        meterRegistry.counter("dispatcher.service.operations", "type", operation).increment();
    }
}
