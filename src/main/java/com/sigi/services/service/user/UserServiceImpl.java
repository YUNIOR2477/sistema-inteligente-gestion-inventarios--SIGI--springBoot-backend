package com.sigi.services.service.user;

import com.sigi.persistence.entity.*;
import com.sigi.persistence.enums.OrderStatus;
import com.sigi.persistence.enums.RoleList;
import com.sigi.persistence.repository.*;
import com.sigi.presentation.dto.auth.NewUserDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.presentation.dto.user.MetricsDto;
import com.sigi.presentation.dto.user.UpdateUserProfileDto;
import com.sigi.presentation.dto.user.UserDto;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static com.sigi.util.Constants.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final DtoMapper dtoMapper;
    private final MeterRegistry meterRegistry;
    private final PersistenceMethod persistenceMethod;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    @CacheEvict(value = {USER_BY_ID, USER_BY_EMAIL, USERS_ACTIVE, USERS_INACTIVE, USER_PROFILE, USERS_INACTIVE_BY_NAME}, allEntries = true)
    public ApiResponse<UserDto> createUser(NewUserDto userDto) {
        log.debug("(createUser) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);

        User response = userRepository.save(User.builder()
                .name(userDto.getName())
                .surname(userDto.getSurname())
                .password(passwordEncoder.encode(userDto.getPassword()))
                .email(userDto.getEmail())
                .phoneNumber(userDto.getPhoneNumber())
                .role(persistenceMethod.getRoleByName(userDto.getRole()))
                .notificationsEnabled(true)
                .chatNotificationsEnabled(true)
                .chatRooms(List.of())
                .active(true)
                .createdAt(LocalDateTime.now(ZoneId.of("America/Bogota")))
                .createdBy(persistenceMethod.getCurrentUserEmail())
                .build());
        recordMetrics(sample, "createUser");
        log.info("(createUser) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(CREATED_SUCCESSFULLY.formatted(USER),
                dtoMapper.toUserDto(response));
    }

    @Override
    @Transactional
    @CacheEvict(value = {USER_BY_ID, USER_BY_EMAIL, USERS_ACTIVE, USERS_INACTIVE, USER_PROFILE, USERS_INACTIVE_BY_NAME}, allEntries = true)
    public ApiResponse<UserDto> updateUser(UUID id, NewUserDto userDto) {
        log.debug("(updateUser) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        User existingUser = persistenceMethod.getUserById(id);
        if (existingUser.getActive().equals(Boolean.FALSE)) {
            throw new IllegalArgumentException(ENTITY_INACTIVE.formatted(USER_ID.formatted(id)));
        }
        existingUser.setName(userDto.getName());
        existingUser.setSurname(userDto.getSurname());
        existingUser.setPhoneNumber(userDto.getPhoneNumber());
        existingUser.setPassword(passwordEncoder.encode(userDto.getPassword()));
        existingUser.setEmail(userDto.getEmail());
        existingUser.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        existingUser.setUpdatedBy(persistenceMethod.getCurrentUserEmail());
        User response = userRepository.save(existingUser);
        recordMetrics(sample, "updateUser");
        log.info("(updateUser) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(UPDATED_SUCCESSFULLY.formatted(USER),
                dtoMapper.toUserDto(response));
    }

    @Override
    @Transactional
    @CacheEvict(value = {USER_BY_ID, USER_BY_EMAIL, USERS_ACTIVE, USERS_INACTIVE, USER_PROFILE, USERS_INACTIVE_BY_NAME}, allEntries = true)
    public ApiResponse<UserDto> updateUserProfile(UpdateUserProfileDto userDto) {
        log.debug("(updateUserProfile) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        User existingUser = persistenceMethod.getCurrentUser();
        existingUser.setName(userDto.getName());
        existingUser.setSurname(userDto.getSurname());
        existingUser.setPhoneNumber(userDto.getPhoneNumber());
        existingUser.setEmail(userDto.getEmail());
        if (userDto.getPassword() != null && !userDto.getPassword().isBlank()) {
            existingUser.setPassword(passwordEncoder.encode(userDto.getPassword()));
        }
        existingUser.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        existingUser.setUpdatedBy(persistenceMethod.getCurrentUserEmail());
        User response = userRepository.save(existingUser);
        recordMetrics(sample, "updateUserProfile");
        log.info("(updateUserProfile) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(
                UPDATED_SUCCESSFULLY.formatted(USER),
                dtoMapper.toUserDto(response)
        );
    }


    @Override
    @Transactional
    @CacheEvict(value = {USER_BY_ID, USER_BY_EMAIL, USERS_ACTIVE, USERS_INACTIVE, USER_PROFILE, USERS_INACTIVE_BY_NAME}, allEntries = true)
    public ApiResponse<Void> deactivateUser(UUID id) {
        log.debug("(deactivateUser) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        User existingUser = persistenceMethod.getUserById(id);
        if (existingUser.getActive().equals(Boolean.FALSE)) {
            throw new IllegalArgumentException(ENTITY_PREVIOUSLY_DELETED.formatted(USER_ID.formatted(id)));
        }
        existingUser.setActive(false);
        existingUser.setUpdatedBy(persistenceMethod.getCurrentUserEmail());
        existingUser.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        existingUser.setDeletedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        existingUser.setDeletedBy(persistenceMethod.getCurrentUserEmail());
        userRepository.save(existingUser);
        recordMetrics(sample, "deactivateUser");
        log.info("(deactivateUser) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(DELETED_SUCCESSFULLY.formatted(USER), null);
    }

    @Override
    @Transactional
    @CacheEvict(value = {USER_BY_ID, USER_BY_EMAIL, USERS_ACTIVE, USERS_INACTIVE, USER_PROFILE, USERS_INACTIVE_BY_NAME}, allEntries = true)
    public ApiResponse<Void> activateUser(UUID id) {
        log.debug("(activateUser) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        User existingUser = persistenceMethod.getUserById(id);
        if (Boolean.TRUE.equals(existingUser.getActive())) {
            throw new IllegalArgumentException(ENTITY_ALREADY_ACTIVE.formatted(USER_ID.formatted(id)));
        }
        existingUser.setActive(true);
        existingUser.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        existingUser.setUpdatedBy(persistenceMethod.getCurrentUserEmail());
        existingUser.setDeletedAt(null);
        existingUser.setDeletedBy(null);
        userRepository.save(existingUser);
        recordMetrics(sample, "activateUser");
        log.info("(activateUser) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(ENTITY_ACTIVATED_SUCCESSFULLY.formatted(USER_ID.formatted(id)), null);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = USER_BY_ID, key = "#id")
    public ApiResponse<UserDto> getUserById(UUID id) {
        log.debug("(getUserById) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        User response = persistenceMethod.getUserById(id);
        if (response.getActive().equals(Boolean.FALSE)) {
            throw new IllegalArgumentException(ENTITY_INACTIVE.formatted(USER_ID.formatted(id)));
        }
        recordMetrics(sample, "getUserById");
        log.info("(getUserById) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(SEARCH_SUCCESSFULLY.formatted(USER),
                dtoMapper.toUserDto(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = USER_BY_ID, key = "#id")
    public ApiResponse<UserDto> getDeletedUserById(UUID id) {
        log.debug("(getDeletedUserById) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        User response = userRepository.findByIdAndActiveFalse(id).orElseThrow(() -> new EntityNotFoundException(ENTITY_NOT_FOUND.formatted(USER_ID.formatted(id))));
        recordMetrics(sample, "getDeletedUserById");
        log.info("(getDeletedUserById) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(SEARCH_SUCCESSFULLY.formatted(ORDER),
                dtoMapper.toUserDto(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = USERS_ACTIVE,  key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<UserDto>> listActiveUsers(PagedRequestDto pagedRequestDto) {
        log.debug("(listActiveUsers) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<User> response = userRepository.findByActiveTrue(PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        log.info("(listActiveUsers) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "listActiveUsers");
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(USERS),
                dtoMapper.toUserDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = USERS_INACTIVE,  key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<UserDto>> listInactiveUsers(PagedRequestDto pagedRequestDto) {
        log.debug("(listInactiveUsers) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<User> response = userRepository.findByActiveFalse(PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        log.info("(listInactiveUsers) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "listInactiveUsers");
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(USERS),
                dtoMapper.toUserDtoPage(response));
    }

    @Override
    public ApiResponse<PagedResponse<UserDto>> listUsersByName(PagedRequestDto pagedRequestDto) {
        log.debug("(listUsersByName) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<User> response = userRepository.findByNameContainingIgnoreCaseAndActiveTrue(pagedRequestDto.getSearchValue(), PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        log.info("(listUsersByName) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "listUsersByName");
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(USERS),
                dtoMapper.toUserDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = USERS_INACTIVE_BY_NAME,  key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<UserDto>> listInactiveUsersByName(PagedRequestDto pagedRequestDto) {
        log.debug("(listInactiveUsersByName) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<User> response = userRepository.findByActiveFalseAndNameContainingIgnoreCase(pagedRequestDto.getSearchValue(), PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        log.info("(listInactiveUsersByName) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "listInactiveUsersByName");
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(USERS),
                dtoMapper.toUserDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = USER_BY_EMAIL, key = "#email")
    public ApiResponse<UserDto> getUserByEmail(String email) {
        log.debug("(getUserByEmail) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        User response = persistenceMethod.getUserByEmail(email);
        if (response.getActive().equals(Boolean.FALSE)) {
            throw new IllegalArgumentException(ENTITY_INACTIVE.formatted(USER_EMAIL.formatted(email)));
        }
        recordMetrics(sample, "getUserByEmail");
        log.info("(getUserByEmail) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(SEARCH_SUCCESSFULLY.formatted(USER),
                dtoMapper.toUserDto(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = {USER_PROFILE})
    public ApiResponse<UserDto> getCurrentUser() {
        log.debug("(getCurrentUser) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        User response = persistenceMethod.getCurrentUser();
        recordMetrics(sample, "getCurrentUser");
        log.info("(getCurrentUser) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(SEARCH_SUCCESSFULLY.formatted(USER),
                dtoMapper.toUserDto(response));
    }

    @Override
    @Transactional
    @CacheEvict(value = {USER_BY_ID, USER_BY_EMAIL, USERS_ACTIVE, USERS_INACTIVE, USER_PROFILE, USERS_INACTIVE_BY_NAME}, allEntries = true)
    public ApiResponse<Void> changeStatusNotification() {
        log.debug("(changeStatusNotification) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        User response = persistenceMethod.getCurrentUser();
        if (response.isNotificationsEnabled()) {
            response.setNotificationsEnabled(false);
            userRepository.save(response);
        } else {
            response.setNotificationsEnabled(true);
            userRepository.save(response);
        }
        recordMetrics(sample, "changeStatusNotification");
        log.info("(changeStatusNotification) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(ENTITY_ACTIVATED_SUCCESSFULLY.formatted("Change notifications status"), null);
    }

    @Override
    @Transactional
    @CacheEvict(value = {USER_BY_ID, USER_BY_EMAIL, USERS_ACTIVE, USERS_INACTIVE, USER_PROFILE, USERS_INACTIVE_BY_NAME}, allEntries = true)
    public ApiResponse<Void> changeStatusNotificationChat() {
        log.debug("(changeStatusNotificationChat) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        User response = persistenceMethod.getCurrentUser();
        if (response.isChatNotificationsEnabled()) {
            response.setChatNotificationsEnabled(false);
            userRepository.save(response);
        } else {
            response.setChatNotificationsEnabled(true);
            userRepository.save(response);
        }
        recordMetrics(sample, "changeStatusNotification");
        log.info("(changeStatusNotificationChat) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(ENTITY_ACTIVATED_SUCCESSFULLY.formatted("Change chat notifications status"), null);
    }

    @Override
    @Transactional
    @CacheEvict(value = {USER_BY_ID, USER_BY_EMAIL, USERS_ACTIVE, USERS_INACTIVE, USER_PROFILE, USERS_INACTIVE_BY_NAME}, allEntries = true)
    public ApiResponse<Void> changeRole(UUID id, RoleList role) {
        log.debug("(changeRole) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        User existingUser = persistenceMethod.getUserById(id);
        if (existingUser.getActive().equals(Boolean.FALSE)) {
            throw new IllegalArgumentException(ENTITY_INACTIVE.formatted(USER_ID.formatted(id)));
        }
        existingUser.setRole(persistenceMethod.getRoleByName(role.name()));
        existingUser.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        existingUser.setUpdatedBy(persistenceMethod.getCurrentUserEmail());
        userRepository.save(existingUser);
        recordMetrics(sample, "changeRole");
        log.info("(changeRole) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(UPDATED_SUCCESSFULLY.formatted("Change role"), null);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = METRICS, key = "")
    public ApiResponse<MetricsDto> getMetrics() {
        log.debug("(getMetrics) -> " + PERFORMING_OPERATION);
        List<Order> orders = orderRepository.findAll();
        List<Product> products = productRepository.findAll();
        List<Inventory> inventories = inventoryRepository.findAll();
        long warehouses = warehouseRepository.count();
        MetricsDto metricsDto = MetricsDto.builder()
                .totalProducts(products.size())
                .totalWarehouses(Math.toIntExact(warehouses))
                .totalInventories(inventories.size())
                .totalLoWStock(
                        (int) inventories.stream()
                                .filter(inventory -> inventory.getAvailableQuantity()
                                        .compareTo(BigDecimal.valueOf(10)) <= 0)
                                .count()
                )
                .totalOrdersPending((int) orders.stream()
                        .filter(order -> order.getStatus() == OrderStatus.PENDING)
                        .count())
                .totalOrdersCanceled((int) orders.stream()
                        .filter(order -> order.getStatus() == OrderStatus.CANCELED)
                        .count())
                .totalOrdersDraft((int) orders.stream()
                        .filter(order -> order.getStatus() == OrderStatus.DRAFT)
                        .count())
                .totalOrdersConfirmed((int) orders.stream()
                        .filter(order -> order.getStatus() == OrderStatus.CONFIRMED)
                        .count())
                .totalOrdersDelivered((int) orders.stream()
                        .filter(order -> order.getStatus() == OrderStatus.DELIVERED)
                        .count())
                .latestProductsAdded(
                        dtoMapper.toProductDtoList(
                                products.isEmpty()
                                        ? List.of()
                                        : products.subList(
                                        Math.max(products.size() - 6, 0),
                                        products.size()
                                )
                        )
                )
                .build();
        Timer.Sample sample = Timer.start(meterRegistry);
        recordMetrics(sample, "getCurrentUser");
        log.info("(getMetrics) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(OPERATION_COMPLETED, metricsDto);
    }

    private void recordMetrics(Timer.Sample sample, String operation) {
        sample.stop(Timer.builder("user.service.latency")
                .tag("operation", operation)
                .register(meterRegistry));
        meterRegistry.counter("user.service.operations", "type", operation).increment();
    }
}
