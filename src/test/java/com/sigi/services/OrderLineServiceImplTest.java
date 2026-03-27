package com.sigi.services;

import com.sigi.persistence.entity.*;
import com.sigi.persistence.repository.OrderLineRepository;
import com.sigi.persistence.repository.OrderRepository;
import com.sigi.presentation.dto.order.NewOrderLineDto;
import com.sigi.presentation.dto.order.OrderLineDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.services.service.order.line.OrderLineServiceImpl;
import com.sigi.services.service.websocket.notification.NotificationService;
import com.sigi.util.DtoMapper;
import com.sigi.util.PersistenceMethod;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.sigi.util.Constants.*;

@ExtendWith(MockitoExtension.class)
class OrderLineServiceImplTest {

    @Mock
    private OrderLineRepository orderLineRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DtoMapper dtoMapper;

    @Mock
    private PersistenceMethod persistenceMethod;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrderLineServiceImpl orderLineService;

    private SimpleMeterRegistry meterRegistry;

    private UUID orderId;
    private UUID orderLineId;
    private UUID inventoryId;
    private UUID productId;
    private Order order;
    private OrderLine orderLine;
    private Inventory inventory;
    private Product product;
    private OrderLineDto orderLineDto;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        orderLineService = new OrderLineServiceImpl(orderLineRepository, orderRepository, dtoMapper, meterRegistry, persistenceMethod, notificationService);

        orderId = UUID.randomUUID();
        orderLineId = UUID.randomUUID();
        inventoryId = UUID.randomUUID();
        productId = UUID.randomUUID();

        product = Product.builder().id(productId).price(BigDecimal.valueOf(10)).name("P1").build();
        Warehouse wh = Warehouse.builder().id(UUID.randomUUID()).name("WH").build();

        inventory = Inventory.builder()
                .id(inventoryId)
                .product(product)
                .warehouse(wh)
                .lot("LOT-1")
                .availableQuantity(BigDecimal.TEN)
                .reservedQuantity(BigDecimal.ZERO)
                .active(true)
                .build();

        order = Order.builder()
                .id(orderId)
                .client(Client.builder().id(UUID.randomUUID()).build())
                .user(User.builder().id(UUID.randomUUID()).build())
                .status(com.sigi.persistence.enums.OrderStatus.DRAFT)
                .total(BigDecimal.ZERO)
                .build();

        orderLine = OrderLine.builder()
                .id(orderLineId)
                .order(order)
                .inventory(inventory)
                .product(product)
                .lot(inventory.getLot())
                .quantity(BigDecimal.valueOf(2))
                .unitPrice(product.getPrice())
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        orderLineDto = OrderLineDto.builder()
                .id(orderLineId)
                .orderId(orderId)
                .lot("LOT-1")
                .quantity(BigDecimal.valueOf(2))
                .unitPrice(product.getPrice())
                .build();

        lenient().when(dtoMapper.toOnlineOrderDto(any(OrderLine.class))).thenReturn(orderLineDto);
        lenient().when(dtoMapper.toOnlineOrderDtoPage(any(Page.class))).thenAnswer(inv -> {
            Page<OrderLine> page = inv.getArgument(0);
            return page.map(l -> orderLineDto);
        });
    }

    // ------------------- createOrderLine -------------------
    @Test
    void shouldCreateOrderLineSuccessfullyAndUpdateOrderTotal() {
        NewOrderLineDto dto = NewOrderLineDto.builder()
                .inventoryId(inventoryId)
                .quantity(BigDecimal.valueOf(3))
                .build();

        when(persistenceMethod.getOrderById(orderId)).thenReturn(order);
        when(persistenceMethod.getInventoryById(inventoryId)).thenReturn(inventory);
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("creator@example.com");
        when(orderLineRepository.save(any(OrderLine.class))).thenAnswer(inv -> {
            OrderLine l = inv.getArgument(0);
            l.setId(orderLineId);
            return l;
        });
        when(orderLineRepository.findByOrderId(orderId)).thenReturn(List.of(orderLine));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(persistenceMethod.getOrderById(orderId)).thenReturn(order);

        ApiResponse<OrderLineDto> response = orderLineService.createOrderLine(orderId, dto);

        assertEquals(200, response.getCode());
        assertEquals(CREATED_SUCCESSFULLY.formatted(ONLINE_ORDER), response.getMessage());
        verify(orderLineRepository, times(1)).save(any(OrderLine.class));
        verify(orderRepository, times(1)).save(order);
        verify(notificationService, times(1)).createNotification(contains("New online order line created"), anyString(), eq(order.getUser().getId()));
        assertTrue(meterRegistry.get("onlineOrder.service.operations").tags("type", "createOrderLine").counter().count() >= 1.0);
    }

    @Test
    void shouldThrowWhenCreatingOrderLineIfOrderNotDraft() {
        order.setStatus(com.sigi.persistence.enums.OrderStatus.CONFIRMED);
        when(persistenceMethod.getOrderById(orderId)).thenReturn(order);

        NewOrderLineDto dto = NewOrderLineDto.builder().inventoryId(inventoryId).quantity(BigDecimal.ONE).build();

        assertThrows(IllegalStateException.class, () -> orderLineService.createOrderLine(orderId, dto));
        verify(orderLineRepository, never()).save(any());
    }

    // ------------------- updateOrderLine -------------------
    @Test
    void shouldUpdateOrderLineSuccessfullyAndRecalculateOrderTotal() {
        NewOrderLineDto dto = NewOrderLineDto.builder()
                .inventoryId(inventoryId)
                .quantity(BigDecimal.valueOf(5))
                .build();

        OrderLine existing = OrderLine.builder()
                .id(orderLineId)
                .order(order)
                .inventory(inventory)
                .product(product)
                .quantity(BigDecimal.valueOf(1))
                .unitPrice(product.getPrice())
                .active(true)
                .build();

        when(persistenceMethod.getOnlineOrderById(orderLineId)).thenReturn(existing);
        when(persistenceMethod.getInventoryById(inventoryId)).thenReturn(inventory);
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("updater@example.com");
        when(orderLineRepository.save(existing)).thenReturn(existing);
        when(orderLineRepository.findByOrderId(order.getId())).thenReturn(List.of(existing));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        ApiResponse<OrderLineDto> response = orderLineService.updateOrderLine(orderLineId, dto);

        assertEquals(200, response.getCode());
        assertEquals(UPDATED_SUCCESSFULLY.formatted(ONLINE_ORDER), response.getMessage());
        assertEquals(BigDecimal.valueOf(5), existing.getQuantity());
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void shouldThrowWhenUpdatingOrderLineIfOrderNotDraft() {
        OrderLine existing = OrderLine.builder().id(orderLineId).order(order).build();
        order.setStatus(com.sigi.persistence.enums.OrderStatus.CONFIRMED);
        when(persistenceMethod.getOnlineOrderById(orderLineId)).thenReturn(existing);

        NewOrderLineDto dto = NewOrderLineDto.builder().inventoryId(inventoryId).quantity(BigDecimal.ONE).build();

        assertThrows(IllegalStateException.class, () -> orderLineService.updateOrderLine(orderLineId, dto));
        verify(orderLineRepository, never()).save(any());
    }

    // ------------------- deleteOrderLine -------------------
    @Test
    void shouldDeleteOrderLineSuccessfullyAndNotify() {
        OrderLine existing = OrderLine.builder().id(orderLineId).order(order).unitPrice(product.getPrice()).quantity(BigDecimal.valueOf(2)).build();
        when(persistenceMethod.getOnlineOrderById(orderLineId)).thenReturn(existing);
        doNothing().when(orderLineRepository).delete(existing);
        when(orderLineRepository.findByOrderId(order.getId())).thenReturn(List.of());
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        ApiResponse<Void> response = orderLineService.deleteOrderLine(orderLineId);

        assertEquals(200, response.getCode());
        assertEquals(DELETED_SUCCESSFULLY.formatted(ONLINE_ORDER), response.getMessage());
        verify(orderLineRepository, times(1)).delete(existing);
        verify(notificationService, times(2)).createNotification(contains("Online order line deleted"), anyString(), any(UUID.class));
    }

    @Test
    void shouldThrowWhenDeletingOrderLineIfOrderNotDraft() {
        OrderLine existing = OrderLine.builder().id(orderLineId).order(order).build();
        order.setStatus(com.sigi.persistence.enums.OrderStatus.CONFIRMED);
        when(persistenceMethod.getOnlineOrderById(orderLineId)).thenReturn(existing);

        assertThrows(IllegalStateException.class, () -> orderLineService.deleteOrderLine(orderLineId));
        verify(orderLineRepository, never()).delete(any());
    }

    // ------------------- restoreOrderLine -------------------
    @Test
    void shouldRestoreOrderLineSuccessfully() {
        OrderLine deleted = OrderLine.builder().id(orderLineId).active(false).build();
        when(persistenceMethod.getOnlineOrderById(orderLineId)).thenReturn(deleted);
        when(orderLineRepository.save(deleted)).thenReturn(deleted);

        ApiResponse<Void> response = orderLineService.restoreOrderLine(orderLineId);

        assertEquals(200, response.getCode());
        assertTrue(deleted.getActive());
        verify(orderLineRepository, times(1)).save(deleted);
    }

    @Test
    void shouldThrowWhenRestoringAlreadyActiveOrderLine() {
        OrderLine active = OrderLine.builder().id(orderLineId).active(true).build();
        when(persistenceMethod.getOnlineOrderById(orderLineId)).thenReturn(active);

        assertThrows(IllegalArgumentException.class, () -> orderLineService.restoreOrderLine(orderLineId));
        verify(orderLineRepository, never()).save(any());
    }

    // ------------------- getOrderLineById / getDeletedOrderLineById -------------------
    @Test
    void shouldGetOrderLineByIdSuccessfullyWhenActive() {
        OrderLine active = OrderLine.builder().id(orderLineId).active(true).build();
        when(persistenceMethod.getOnlineOrderById(orderLineId)).thenReturn(active);

        ApiResponse<OrderLineDto> response = orderLineService.getOrderLineById(orderLineId);

        assertEquals(200, response.getCode());
        assertEquals(orderLineDto, response.getData());
    }

    @Test
    void shouldThrowWhenGetOrderLineByIdIfInactive() {
        OrderLine inactive = OrderLine.builder().id(orderLineId).active(false).build();
        when(persistenceMethod.getOnlineOrderById(orderLineId)).thenReturn(inactive);

        assertThrows(IllegalArgumentException.class, () -> orderLineService.getOrderLineById(orderLineId));
    }

    @Test
    void shouldGetDeletedOrderLineByIdSuccessfully() {
        OrderLine deleted = OrderLine.builder().id(orderLineId).active(false).build();
        when(orderLineRepository.findByIdAndActiveFalse(orderLineId)).thenReturn(Optional.of(deleted));

        ApiResponse<OrderLineDto> response = orderLineService.getDeletedOrderLineById(orderLineId);

        assertEquals(200, response.getCode());
        assertEquals(orderLineDto, response.getData());
    }

    @Test
    void shouldThrowWhenDeletedOrderLineNotFound() {
        when(orderLineRepository.findByIdAndActiveFalse(orderLineId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> orderLineService.getDeletedOrderLineById(orderLineId));
    }

    // ------------------- list paginados -------------------
    @Test
    void shouldListAllActiveLinesSuccessfully() {
        PagedRequestDto req = PagedRequestDto.builder().page(0).size(10).sortDirection("desc").sortField("createdAt").build();
        Page<OrderLine> page = new PageImpl<>(List.of(orderLine), PageRequest.of(0,10), 1);
        when(orderLineRepository.findByActiveTrue(any(Pageable.class))).thenReturn(page);

        ApiResponse<PagedResponse<OrderLineDto>> response = orderLineService.listAllActiveLines(req);

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().getTotalElements());
    }
}