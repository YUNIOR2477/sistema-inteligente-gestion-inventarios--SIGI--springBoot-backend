package com.sigi.services.service.order;

import com.sigi.persistence.entity.*;
import com.sigi.persistence.enums.OrderStatus;
import com.sigi.persistence.repository.OrderRepository;
import com.sigi.presentation.dto.order.NewOrderDto;
import com.sigi.presentation.dto.order.OrderDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.services.service.inventory.InventoryService;
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
import java.util.UUID;

import static com.sigi.util.Constants.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final PersistenceMethod persistenceMethod;
    private final DtoMapper dtoMapper;
    private final MeterRegistry meterRegistry;
    private final NotificationService notificationService;

    @Override
    @Transactional
    @CacheEvict(value = {ORDER_BY_ID, ORDER_CALCULATE, ORDERS_BY_CLIENT, ORDERS_BY_USER, ALL_ACTIVE_ORDERS, ALL_DELETED_ORDERS}, allEntries = true)
    public ApiResponse<OrderDto> createOrder(NewOrderDto dto) {
        log.debug("(createOrder) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Client client = persistenceMethod.getClientById(dto.getClientId());
        User user = persistenceMethod.getCurrentUser();
        Dispatcher dispatcher = persistenceMethod.getDispatcherById(dto.getDispatcherId());
        Warehouse warehouse = persistenceMethod.getWarehouseById(dto.getWarehouseId());
        Order order = Order.builder()
                .client(client)
                .user(user)
                .warehouse(warehouse)
                .dispatcher(dispatcher)
                .status(OrderStatus.DRAFT)
                .total(BigDecimal.ZERO)
                .active(true)
                .createdAt(LocalDateTime.now(ZoneId.of("America/Bogota")))
                .createdBy(persistenceMethod.getCurrentUserEmail())
                .build();
        Order saved = orderRepository.save(order);
        notificationService.createNotification("New Order Created", "Order ID: " + saved.getId(), user.getId());
        recordMetrics(sample, "createOrder");
        log.info("(createOrder) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(CREATED_SUCCESSFULLY.formatted(ORDER),
                dtoMapper.toOrderDto(saved));
    }

    @Override
    @Transactional
    @CacheEvict(value = {ORDER_BY_ID, ORDER_CALCULATE, ORDERS_BY_CLIENT, ORDERS_BY_USER, ALL_ACTIVE_ORDERS, ALL_DELETED_ORDERS}, allEntries = true)
    public ApiResponse<OrderDto> updateOrder(UUID id, NewOrderDto dto) {
        log.debug("(updateOrder) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Order order = persistenceMethod.getOrderById(id);
        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new IllegalStateException("Only draft orders can be updated");
        }
        Client client = persistenceMethod.getClientById(dto.getClientId());
        User user = persistenceMethod.getCurrentUser();
        Dispatcher dispatcher = persistenceMethod.getDispatcherById(dto.getDispatcherId());
        Warehouse warehouse = persistenceMethod.getWarehouseById(dto.getWarehouseId());
        order.setClient(client);
        order.setUser(user);
        order.setDispatcher(dispatcher);
        order.setWarehouse(warehouse);
        order.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        order.setUpdatedBy(persistenceMethod.getCurrentUserEmail());
        Order updated = orderRepository.save(order);
        notificationService.createNotification("Order Updated", "Order ID: " + updated.getId(), user.getId());
        recordMetrics(sample, "updateOrder");
        log.info("(updateOrder) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(UPDATED_SUCCESSFULLY.formatted(ORDER),
                dtoMapper.toOrderDto(updated));
    }

    @Override
    @Transactional
    @CacheEvict(value = {ORDER_BY_ID, ORDER_CALCULATE, ORDERS_BY_CLIENT, ORDERS_BY_USER, ALL_ACTIVE_ORDERS, ALL_DELETED_ORDERS}, allEntries = true)
    public ApiResponse<Void> deleteOrder(UUID id) {
        log.debug("(deleteOrder) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Order order = persistenceMethod.getOrderById(id);
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            for (OrderLine line : order.getLines()) {
                inventoryService.releaseInventoryReservation(line.getInventory().getId(), line.getQuantity(), order.getId(), order.getUser().getId(), "Order deleted");
            }
        }
        order.setActive(false);
        order.setDeletedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        order.setDeletedBy(persistenceMethod.getCurrentUserEmail());
        orderRepository.save(order);
        notificationService.createNotification("Order Deleted", "Order ID: " + order.getId(), order.getUser().getId());
        recordMetrics(sample, "deleteOrder");
        log.info("(deleteOrder) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(DELETED_SUCCESSFULLY.formatted(ORDER), null);
    }

    @Override
    @Transactional
    @CacheEvict(value = {ORDER_BY_ID, ORDER_CALCULATE, ORDERS_BY_CLIENT, ORDERS_BY_USER, ALL_ACTIVE_ORDERS, ALL_DELETED_ORDERS}, allEntries = true)
    public ApiResponse<Void> restoreOrder(UUID id) {
        log.debug("(restoreOrder) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Order order = persistenceMethod.getOrderById(id);
        if (order.getActive().equals(Boolean.TRUE)) {
            throw new IllegalArgumentException(ENTITY_ALREADY_ACTIVE.formatted(ORDER_ID.formatted(id)));
        }
        order.setActive(true);
        order.setDeletedAt(null);
        order.setDeletedBy(null);
        orderRepository.save(order);
        notificationService.createNotification("Order Restored", "Order ID: " + order.getId(), order.getUser().getId());
        recordMetrics(sample, "restoreOrder");
        log.info("(restoreOrder) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(ENTITY_ACTIVATED_SUCCESSFULLY.formatted(ORDER), null);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<OrderDto> getOrderById(UUID id) {
        log.debug("(getOrderById) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Order response = persistenceMethod.getOrderById(id);
        if (response.getActive().equals(Boolean.FALSE)) {
            throw new IllegalArgumentException(ENTITY_INACTIVE.formatted(ORDER_ID.formatted(id)));
        }
        recordMetrics(sample, "getOrderById");
        log.info("(getOrderById) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(SEARCH_SUCCESSFULLY.formatted(ORDER),
                dtoMapper.toOrderDto(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ORDER_BY_ID, key = "#id")
    public ApiResponse<OrderDto> getDeletedOrderById(UUID id) {
        log.debug("(getDeletedOrderById) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Order response = orderRepository.findByIdAndActiveFalse(id).orElseThrow(() -> new EntityNotFoundException(ENTITY_NOT_FOUND.formatted(ORDER_ID.formatted(id))));
        recordMetrics(sample, "getDeletedOrderById");
        log.info("(getDeletedOrderById) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(SEARCH_SUCCESSFULLY.formatted(ORDER),
                dtoMapper.toOrderDto(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ALL_ACTIVE_ORDERS, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<OrderDto>> listAllActiveOrders(PagedRequestDto pagedRequestDto) {
        log.debug("(listAllActiveOrders) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Order> response = orderRepository.findByActiveTrue(PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        log.info("(listAllActiveOrders) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "listAllActiveOrders");
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(ORDERS),
                dtoMapper.toOrderDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ALL_DELETED_ORDERS, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<OrderDto>> listAllDeletedOrders(PagedRequestDto pagedRequestDto) {
        log.debug("(listAllDeletedOrders) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Order> response = orderRepository.findByActiveFalse(PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        log.info("(listAllDeletedOrders) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "listAllDeletedOrders");
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(ORDERS),
                dtoMapper.toOrderDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ORDERS_BY_CLIENT, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<OrderDto>> listOrdersByClient(PagedRequestDto pagedRequestDto) {
        log.debug("(listInvoicesByClient) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Order> response = orderRepository.findByClientIdAndActiveTrue(pagedRequestDto.getSearchId(), PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        log.info("(listInvoicesByClient) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "listInvoicesByClient");
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(ORDERS),
                dtoMapper.toOrderDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<PagedResponse<OrderDto>> listOrdersByClientName(PagedRequestDto pagedRequestDto) {
        log.debug("(listOrdersByClientName) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Order> response = orderRepository.findByClientNameContainingIgnoreCaseAndActiveTrueAndStatus(pagedRequestDto.getSearchValue(), OrderStatus.DRAFT, PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        log.info("(listOrdersByClientName) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "listOrdersByClientName");
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(ORDERS),
                dtoMapper.toOrderDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ORDERS_BY_USER, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<OrderDto>> listOrdersByUser(PagedRequestDto pagedRequestDto) {
        log.debug("(listMovementsByUser) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Order> response = orderRepository.findByUserEmailContainingIgnoreCaseAndActiveTrue(pagedRequestDto.getSearchValue(), PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        log.info("(listMovementsByUser) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "listMovementsByUser");
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(ORDERS),
                dtoMapper.toOrderDtoPage(response));
    }

    @Transactional(readOnly = true)
    @Override
    public ApiResponse<PagedResponse<OrderDto>> listOrdersByInventory(PagedRequestDto pagedRequestDto) {
        log.debug("(listOrdersByInventory) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Order> response = orderRepository.findOrdersByInventoryId(pagedRequestDto.getSearchId(), PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        log.info("(listOrdersByInventory) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "listOrdersByInventory");
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(ORDERS),
                dtoMapper.toOrderDtoPage(response));
    }

    @Override
    @Transactional
    @CacheEvict(value = {ORDER_BY_ID, ORDER_CALCULATE, ORDERS_BY_CLIENT, ORDERS_BY_USER, ALL_ACTIVE_ORDERS, ALL_DELETED_ORDERS}, allEntries = true)
    public ApiResponse<Void> cancelOrder(UUID id) {
        log.debug("(cancelOrder) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Order order = persistenceMethod.getOrderById(id);
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            for (OrderLine line : order.getLines()) {
                inventoryService.releaseInventoryReservation(line.getInventory().getId(), line.getQuantity(), order.getId(), order.getUser().getId(), "Order canceled");
            }
        }
        order.setStatus(OrderStatus.CANCELED);
        order.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        order.setUpdatedBy(persistenceMethod.getCurrentUserEmail());
        orderRepository.save(order);
        notificationService.createNotification("Order Canceled", "Order ID: " + order.getId(), order.getUser().getId());
        recordMetrics(sample, "cancelOrder");
        log.info("(cancelOrder) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(UPDATED_SUCCESSFULLY.formatted(ORDER), null);
    }

    @Override
    @Transactional
    @CacheEvict(value = {ORDER_BY_ID, ORDER_CALCULATE, ORDERS_BY_CLIENT, ORDERS_BY_USER, ALL_ACTIVE_ORDERS, ALL_DELETED_ORDERS}, allEntries = true)
    public ApiResponse<Void> changeOrderStatus(UUID id, String status) {
        log.debug("(changeOrderStatus) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        OrderStatus status1 = OrderStatus.valueOf(status);
        Order order = persistenceMethod.getOrderById(id);
        switch (status1) {
            case CONFIRMED:
                if (order.getStatus() != OrderStatus.DRAFT) {
                    throw new IllegalStateException("Only draft orders can be confirmed");
                }
                for (OrderLine line : order.getLines()) {
                    if (line.getInventory() == null || line.getInventory().getId() == null) {
                        throw new IllegalArgumentException("Order line missing inventory reference for product " + line.getProduct().getId());
                    }
                    inventoryService.reserveInventoryStock(
                            line.getInventory().getId(),
                            line.getQuantity(),
                            order.getId(),
                            order.getUser() == null ? null : order.getUser().getId(),
                            "Reserve for order " + order.getId()
                    );
                }
                order.setStatus(OrderStatus.CONFIRMED);
                break;
            case DELIVERED:
                if (order.getStatus() != OrderStatus.CONFIRMED) {
                    throw new IllegalStateException("Only confirmed orders can be delivered");
                }
                for (OrderLine line : order.getLines()) {
                    if (line.getInventory() == null || line.getInventory().getId() == null) {
                        throw new IllegalArgumentException("Order line missing inventory reference for product " + line.getProduct().getId());
                    }
                    inventoryService.registerInventoryExit(order.getId());
                }
                order.setStatus(OrderStatus.DELIVERED);
                break;
            case CANCELED:
                if (order.getStatus() == OrderStatus.CONFIRMED) {
                    for (OrderLine line : order.getLines()) {
                        if (line.getInventory() == null || line.getInventory().getId() == null) {
                            // Si no hay inventory asociado, saltar o lanzar según tu política
                            throw new IllegalArgumentException("Order line missing inventory reference for product " + line.getProduct().getId());
                        }
                        inventoryService.releaseInventoryReservation(
                                line.getInventory().getId(),
                                line.getQuantity(),
                                order.getId(),
                                order.getUser() == null ? null : order.getUser().getId(),
                                "Order canceled " + order.getId()
                        );
                    }
                }
                order.setStatus(OrderStatus.CANCELED);
                break;
            default:
                throw new IllegalArgumentException("Unsupported status: " + status);
        }
        order.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        order.setUpdatedBy(persistenceMethod.getCurrentUserEmail());
        orderRepository.save(order);
        recordMetrics(sample, "changeOrderStatus");
        log.info("(changeOrderStatus) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(UPDATED_SUCCESSFULLY.formatted(ORDER), null);
    }

    private void recordMetrics(Timer.Sample sample, String operation) {
        sample.stop(Timer.builder("order.service.latency")
                .tag("operation", operation)
                .register(meterRegistry));
        meterRegistry.counter("order.service.operations", "type", operation).increment();
    }

}

