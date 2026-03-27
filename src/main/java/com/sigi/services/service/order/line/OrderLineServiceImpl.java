package com.sigi.services.service.order.line;

import com.sigi.persistence.entity.Inventory;
import com.sigi.persistence.entity.OrderLine;
import com.sigi.persistence.entity.Order;
import com.sigi.persistence.repository.OrderLineRepository;
import com.sigi.persistence.repository.OrderRepository;
import com.sigi.presentation.dto.order.NewOrderLineDto;
import com.sigi.presentation.dto.order.OrderLineDto;
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

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderLineServiceImpl implements OrderLineService {

    private final OrderLineRepository orderLineRepository;
    private final OrderRepository orderRepository;
    private final DtoMapper dtoMapper;
    private final MeterRegistry meterRegistry;
    private final PersistenceMethod persistenceMethod;
    private final NotificationService notificationService;

    @Override
    @Transactional
    @CacheEvict(value = {LINE_ORDERS_BY_ORDER, ALL_ACTIVE_LINE_ORDERS, ALL_DELETED_LINE_ORDERS,
            LINE_ORDERS_BY_PRODUCT, LINE_ORDERS_DELETED_BY_PRODUCT, ORDER_BY_ID, ORDER_CALCULATE, ORDERS_BY_CLIENT, ORDERS_BY_USER, ALL_ACTIVE_ORDERS, ALL_DELETED_ORDERS}, allEntries = true)
    public ApiResponse<OrderLineDto> createOrderLine(UUID orderId, NewOrderLineDto dto) {
        log.debug("(createOrderLine) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);

        Order order = persistenceMethod.getOrderById(orderId);
        if (order.getStatus() != com.sigi.persistence.enums.OrderStatus.DRAFT) {
            throw new IllegalStateException("Only draft orders can be modified");
        }

        Inventory inventory = persistenceMethod.getInventoryById(dto.getInventoryId());

        OrderLine line = OrderLine.builder()
                .order(order)
                .product(inventory.getProduct())
                .inventory(inventory)
                .lot(inventory.getLot())
                .quantity(dto.getQuantity())
                .unitPrice(inventory.getProduct().getPrice())
                .active(true)
                .createdAt(LocalDateTime.now(ZoneId.of("America/Bogota")))
                .createdBy(persistenceMethod.getCurrentUserEmail())
                .build();

        OrderLine saved = orderLineRepository.save(line);

        List<OrderLine> lines = orderLineRepository.findByOrderId(orderId);
        BigDecimal total = lines.stream()
                .map(l -> l.getUnitPrice().multiply(l.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotal(total);
        orderRepository.save(order);

        notificationService.createNotification(
                "New online order line created",
                "A new line has been added to order #" + order.getId(),
                order.getUser().getId()
        );

        recordMetrics(sample, "createOrderLine");
        log.info("(createOrderLine) -> " + OPERATION_COMPLETED);

        return ApiResponse.success(
                CREATED_SUCCESSFULLY.formatted(ONLINE_ORDER),
                dtoMapper.toOnlineOrderDto(saved)
        );
    }

    @Override
    @Transactional
    @CacheEvict(value = {LINE_ORDER_BY_ID, LINE_ORDERS_BY_ORDER, ALL_ACTIVE_LINE_ORDERS, ALL_DELETED_LINE_ORDERS, LINE_ORDERS_BY_PRODUCT, LINE_ORDERS_DELETED_BY_PRODUCT, ORDER_BY_ID, ORDER_CALCULATE, ORDERS_BY_CLIENT, ORDERS_BY_USER, ALL_ACTIVE_ORDERS, ALL_DELETED_ORDERS}, allEntries = true)
    public ApiResponse<OrderLineDto> updateOrderLine(UUID id, NewOrderLineDto dto) {
        log.debug("(updateOrderLine) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        OrderLine line = persistenceMethod.getOnlineOrderById(id);
        Order order = line.getOrder();
        if (order.getStatus() != com.sigi.persistence.enums.OrderStatus.DRAFT) {
            throw new IllegalStateException("Only draft orders can be modified");
        }
        Inventory inventory = persistenceMethod.getInventoryById(dto.getInventoryId());
        line.setProduct(inventory.getProduct());
        line.setInventory(inventory);
        line.setLot(inventory.getLot());
        line.setQuantity(dto.getQuantity());
        line.setUnitPrice(inventory.getProduct().getPrice());
        line.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        line.setUpdatedBy(persistenceMethod.getCurrentUserEmail());
        OrderLine updated = orderLineRepository.save(line);
        List<OrderLine> lines = orderLineRepository.findByOrderId(updated.getOrder().getId());
        BigDecimal total = lines.stream()
                .map(l -> l.getUnitPrice().multiply(l.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotal(total);
        orderRepository.save(order);
        recordMetrics(sample, "updateOrderLine");
        log.info("(updateOrderLine) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(UPDATED_SUCCESSFULLY.formatted(ONLINE_ORDER),
                dtoMapper.toOnlineOrderDto(updated));
    }

    @Override
    @Transactional
    @CacheEvict(value = {LINE_ORDERS_BY_ORDER, ALL_ACTIVE_LINE_ORDERS, ALL_DELETED_LINE_ORDERS, LINE_ORDERS_BY_PRODUCT, LINE_ORDERS_DELETED_BY_PRODUCT, ORDER_BY_ID, ORDER_CALCULATE, ORDERS_BY_CLIENT, ORDERS_BY_USER, ALL_ACTIVE_ORDERS, ALL_DELETED_ORDERS}, allEntries = true)
    public ApiResponse<Void> deleteOrderLine(UUID id) {
        log.debug("(deleteOrderLine) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        OrderLine line = persistenceMethod.getOnlineOrderById(id);
        Order order = line.getOrder();
        if (order.getStatus() != com.sigi.persistence.enums.OrderStatus.DRAFT) {
            throw new IllegalStateException("Only draft orders can be modified");
        }
        orderLineRepository.delete(line);
        List<OrderLine> lines = orderLineRepository.findByOrderId(order.getId());
        BigDecimal total = lines.stream()
                .map(l -> l.getUnitPrice().multiply(l.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotal(total);
        orderRepository.save(order);
        notificationService.createNotification(
                "Online order line deleted",
                "A line has been removed from order #" + order.getId(),
                order.getUser().getId()
        );
        notificationService.createNotification(
                "Online order line deleted",
                "A line has been removed from order #" + order.getId(),
                order.getClient().getId()
        );
        recordMetrics(sample, "deleteOrderLine");
        log.info("(deleteOrderLine) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(DELETED_SUCCESSFULLY.formatted(ONLINE_ORDER), null);
    }

    @Override
    @Transactional
    @CacheEvict(value = {LINE_ORDERS_BY_ORDER, ALL_ACTIVE_LINE_ORDERS, ALL_DELETED_LINE_ORDERS, LINE_ORDERS_BY_PRODUCT, LINE_ORDERS_DELETED_BY_PRODUCT, ORDER_BY_ID, ORDER_CALCULATE, ORDERS_BY_CLIENT, ORDERS_BY_USER, ALL_ACTIVE_ORDERS, ALL_DELETED_ORDERS}, allEntries = true)
    public ApiResponse<Void> restoreOrderLine(UUID id) {
        log.debug("(restoreOrderLine) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        OrderLine line = persistenceMethod.getOnlineOrderById(id);
        if (line.getActive().equals(Boolean.TRUE)) {
            throw new IllegalArgumentException(ENTITY_ALREADY_ACTIVE.formatted(ONLINE_ORDER_ID.formatted(id)));
        }
        line.setActive(true);
        line.setDeletedAt(null);
        line.setDeletedBy(null);
        orderLineRepository.save(line);
        recordMetrics(sample, "restoreOrderLine");
        log.info("(restoreOrderLine) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(ENTITY_ACTIVATED_SUCCESSFULLY.formatted(ONLINE_ORDER), null);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = LINE_ORDER_BY_ID, key = "#id")
    public ApiResponse<OrderLineDto> getOrderLineById(UUID id) {
        log.debug("(getOrderLineById) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        OrderLine line = persistenceMethod.getOnlineOrderById(id);
        if (line.getActive().equals(Boolean.FALSE)) {
            throw new IllegalArgumentException(ENTITY_INACTIVE.formatted(ONLINE_ORDER_ID.formatted(id)));
        }
        recordMetrics(sample, "getOrderLineById");
        log.info("(getOrderLineById) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(SEARCH_SUCCESSFULLY.formatted(ONLINE_ORDER),
                dtoMapper.toOnlineOrderDto(line));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = LINE_ORDER_BY_ID, key = "#id")
    public ApiResponse<OrderLineDto> getDeletedOrderLineById(UUID id) {
        log.debug("(getDeletedOrderById) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        OrderLine response = orderLineRepository.findByIdAndActiveFalse(id).orElseThrow(() -> new EntityNotFoundException(ENTITY_NOT_FOUND.formatted(ONLINE_ORDER_ID.formatted(id))));
        recordMetrics(sample, "getDeletedOrderById");
        log.info("(getDeletedOrderById) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(SEARCH_SUCCESSFULLY.formatted(ORDER),
                dtoMapper.toOnlineOrderDto(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ALL_ACTIVE_LINE_ORDERS, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<OrderLineDto>> listAllActiveLines(PagedRequestDto pagedRequestDto) {
        log.debug("(listAllActiveLines) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<OrderLine> response = orderLineRepository.findByActiveTrue(PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        log.info("(listAllActiveLines) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "listAllActiveLines");
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(ONLINE_ORDERS),
                dtoMapper.toOnlineOrderDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ALL_DELETED_LINE_ORDERS, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<OrderLineDto>> listAllDeletedLines(PagedRequestDto pagedRequestDto) {
        log.debug("(listAllDeletedLines) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<OrderLine> response = orderLineRepository.findByActiveFalse(PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        log.info("(listAllDeletedLines) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "listAllDeletedLines");
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(ONLINE_ORDERS),
                dtoMapper.toOnlineOrderDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = LINE_ORDERS_BY_ORDER, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<OrderLineDto>> listLinesByOrder(PagedRequestDto pagedRequestDto) {
        log.debug("(listLinesByOrder) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<OrderLine> response = orderLineRepository.findByOrderIdAndActiveTrue(pagedRequestDto.getSearchId(), PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        log.info("(listLinesByOrder) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "listLinesByOrder");
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(ONLINE_ORDERS),
                dtoMapper.toOnlineOrderDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = LINE_ORDERS_BY_PRODUCT, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<OrderLineDto>> listActiveLinesByProductName(PagedRequestDto pagedRequestDto) {
        log.debug("(listActiveLinesByProductName) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<OrderLine> response = orderLineRepository.findByProductNameContainingIgnoreCaseAndActiveTrue(pagedRequestDto.getSearchValue(), PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        log.info("(listActiveLinesByProductName) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "listActiveLinesByProductName");
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(ONLINE_ORDERS),
                dtoMapper.toOnlineOrderDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = LINE_ORDERS_DELETED_BY_PRODUCT, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<OrderLineDto>> listDeletedLinesByProductName(PagedRequestDto pagedRequestDto) {
        log.debug("(listDeletedLinesByProductName) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<OrderLine> response = orderLineRepository.findByProductNameContainingIgnoreCaseAndActiveFalse(pagedRequestDto.getSearchValue(), PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        log.info("(listDeletedLinesByProductName) -> " + OPERATION_COMPLETED);
        recordMetrics(sample, "listDeletedLinesByProductName");
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(ONLINE_ORDERS),
                dtoMapper.toOnlineOrderDtoPage(response));
    }

    private void recordMetrics(Timer.Sample sample, String operation) {
        sample.stop(Timer.builder("onlineOrder.service.latency")
                .tag("operation", operation)
                .register(meterRegistry));
        meterRegistry.counter("onlineOrder.service.operations", "type", operation).increment();
    }

}
