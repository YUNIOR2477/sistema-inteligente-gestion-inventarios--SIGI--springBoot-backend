package com.sigi.services;

import com.sigi.persistence.entity.*;
import com.sigi.persistence.enums.OrderStatus;
import com.sigi.persistence.repository.OrderRepository;
import com.sigi.presentation.dto.order.NewOrderDto;
import com.sigi.presentation.dto.order.OrderDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.services.service.inventory.InventoryService;
import com.sigi.services.service.order.OrderServiceImpl;
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
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private PersistenceMethod persistenceMethod;

    @Mock
    private DtoMapper dtoMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private OrderServiceImpl orderService;

    private SimpleMeterRegistry meterRegistry;

    private UUID orderId;
    private UUID clientId;
    private UUID userId;
    private UUID warehouseId;
    private UUID dispatcherId;
    private Order order;
    private Client client;
    private User user;
    private Warehouse warehouse;
    private Dispatcher dispatcher;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        orderService = new OrderServiceImpl(orderRepository, inventoryService, persistenceMethod, dtoMapper, meterRegistry, notificationService);

        orderId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        userId = UUID.randomUUID();
        warehouseId = UUID.randomUUID();
        dispatcherId = UUID.randomUUID();

        client = Client.builder().id(clientId).name("ACME").build();
        user = User.builder().id(userId).build();
        warehouse = Warehouse.builder().id(warehouseId).name("Main WH").build();
        dispatcher = Dispatcher.builder().id(dispatcherId).name("Disp").build();

        order = Order.builder()
                .id(orderId)
                .client(client)
                .user(user)
                .warehouse(warehouse)
                .dispatcher(dispatcher)
                .status(OrderStatus.DRAFT)
                .total(BigDecimal.ZERO)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        lenient().when(dtoMapper.toOrderDto(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            return com.sigi.presentation.dto.order.OrderDto.builder()
                    .id(o.getId())
                    .total(o.getTotal())
                    .status(o.getStatus().name())
                    .build();
        });
    }

    // ------------------- createOrder -------------------
    @Test
    void shouldCreateOrderSuccessfullyAndNotify() {
        NewOrderDto dto = NewOrderDto.builder()
                .clientId(clientId)
                .warehouseId(warehouseId)
                .dispatcherId(dispatcherId)
                .build();

        when(persistenceMethod.getClientById(clientId)).thenReturn(client);
        when(persistenceMethod.getCurrentUser()).thenReturn(user);
        when(persistenceMethod.getDispatcherById(dispatcherId)).thenReturn(dispatcher);
        when(persistenceMethod.getWarehouseById(warehouseId)).thenReturn(warehouse);
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("creator@example.com");
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(orderId);
            return o;
        });

        ApiResponse<com.sigi.presentation.dto.order.OrderDto> response = orderService.createOrder(dto);

        assertEquals(200, response.getCode());
        assertEquals(CREATED_SUCCESSFULLY.formatted(ORDER), response.getMessage());
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(notificationService, times(1)).createNotification(eq("New Order Created"), contains("Order ID"), eq(user.getId()));
        assertTrue(meterRegistry.get("order.service.operations").tags("type", "createOrder").counter().count() >= 1.0);
    }

    // ------------------- updateOrder -------------------
    @Test
    void shouldUpdateOrderSuccessfullyWhenDraft() {
        NewOrderDto dto = NewOrderDto.builder()
                .clientId(clientId)
                .warehouseId(warehouseId)
                .dispatcherId(dispatcherId)
                .build();

        when(persistenceMethod.getOrderById(orderId)).thenReturn(order);
        when(persistenceMethod.getClientById(clientId)).thenReturn(client);
        when(persistenceMethod.getCurrentUser()).thenReturn(user);
        when(persistenceMethod.getDispatcherById(dispatcherId)).thenReturn(dispatcher);
        when(persistenceMethod.getWarehouseById(warehouseId)).thenReturn(warehouse);
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("updater@example.com");
        when(orderRepository.save(order)).thenReturn(order);

        ApiResponse<com.sigi.presentation.dto.order.OrderDto> response = orderService.updateOrder(orderId, dto);

        assertEquals(200, response.getCode());
        assertEquals(UPDATED_SUCCESSFULLY.formatted(ORDER), response.getMessage());
        verify(orderRepository, times(1)).save(order);
        assertNotNull(order.getUpdatedAt());
        assertEquals("updater@example.com", order.getUpdatedBy());
        verify(notificationService, times(1)).createNotification(eq("Order Updated"), contains("Order ID"), eq(user.getId()));
    }

    @Test
    void shouldThrowWhenUpdatingNonDraftOrder() {
        order.setStatus(OrderStatus.CONFIRMED);
        when(persistenceMethod.getOrderById(orderId)).thenReturn(order);

        NewOrderDto dto = NewOrderDto.builder().clientId(clientId).warehouseId(warehouseId).dispatcherId(dispatcherId).build();

        assertThrows(IllegalStateException.class, () -> orderService.updateOrder(orderId, dto));
        verify(orderRepository, never()).save(any());
    }

    // ------------------- deleteOrder -------------------
    @Test
    void shouldDeleteOrderAndReleaseReservationsWhenConfirmed() {
        // prepare order with confirmed status and one line
        order.setStatus(OrderStatus.CONFIRMED);
        Inventory inv = Inventory.builder().id(UUID.randomUUID()).build();
        OrderLine line = OrderLine.builder().inventory(inv).quantity(BigDecimal.valueOf(2)).order(order).build();
        order.setLines(List.of(line));

        when(persistenceMethod.getOrderById(orderId)).thenReturn(order);
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("deleter@example.com");
        when(orderRepository.save(order)).thenReturn(order);

        ApiResponse<Void> response = orderService.deleteOrder(orderId);

        assertEquals(200, response.getCode());
        assertEquals(DELETED_SUCCESSFULLY.formatted(ORDER), response.getMessage());
        // verify reservation release called for each line
        verify(inventoryService, times(1)).releaseInventoryReservation(eq(inv.getId()), eq(line.getQuantity()), eq(order.getId()), eq(order.getUser().getId()), contains("Order deleted"));
        assertFalse(order.getActive());
        verify(notificationService, times(1)).createNotification(eq("Order Deleted"), contains("Order ID"), eq(order.getUser().getId()));
    }

    @Test
    void shouldDeleteOrderWithoutReleasingWhenNotConfirmed() {
        order.setStatus(OrderStatus.DRAFT);
        when(persistenceMethod.getOrderById(orderId)).thenReturn(order);
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("deleter@example.com");
        when(orderRepository.save(order)).thenReturn(order);

        ApiResponse<Void> response = orderService.deleteOrder(orderId);

        assertEquals(200, response.getCode());
        verify(inventoryService, never()).releaseInventoryReservation(any(), any(), any(), any(), anyString());
        assertFalse(order.getActive());
    }

    // ------------------- restoreOrder -------------------
    @Test
    void shouldRestoreOrderSuccessfully() {
        Order deleted = Order.builder().id(orderId).active(false).user(user).build();
        when(persistenceMethod.getOrderById(orderId)).thenReturn(deleted);
        when(orderRepository.save(deleted)).thenReturn(deleted);

        ApiResponse<Void> response = orderService.restoreOrder(orderId);

        assertEquals(200, response.getCode());
        assertTrue(deleted.getActive());
        verify(notificationService, times(1)).createNotification(eq("Order Restored"), contains("Order ID"), eq(user.getId()));
    }

    @Test
    void shouldThrowWhenRestoringAlreadyActiveOrder() {
        Order active = Order.builder().id(orderId).active(true).build();
        when(persistenceMethod.getOrderById(orderId)).thenReturn(active);

        assertThrows(IllegalArgumentException.class, () -> orderService.restoreOrder(orderId));
        verify(orderRepository, never()).save(any());
    }

    // ------------------- getOrderById / getDeletedOrderById -------------------
    @Test
    void shouldGetOrderByIdWhenActive() {
        Order active = Order.builder().id(orderId).active(true).status(OrderStatus.CANCELED).build();
        when(persistenceMethod.getOrderById(orderId)).thenReturn(active);

        ApiResponse<com.sigi.presentation.dto.order.OrderDto> response = orderService.getOrderById(orderId);

        assertEquals(200, response.getCode());
        assertEquals("Order has been retrieved successfully.", response.getMessage().contains("Order has been retrieved successfully.") ? response.getMessage() : response.getMessage());
    }

    @Test
    void shouldThrowWhenGetOrderByIdIfInactive() {
        Order inactive = Order.builder().id(orderId).active(false).build();
        when(persistenceMethod.getOrderById(orderId)).thenReturn(inactive);

        assertThrows(IllegalArgumentException.class, () -> orderService.getOrderById(orderId));
    }

    @Test
    void shouldGetDeletedOrderByIdSuccessfully() {
        Order deleted = Order.builder().id(orderId).active(false).status(OrderStatus.CANCELED).build();
        when(orderRepository.findByIdAndActiveFalse(orderId)).thenReturn(Optional.of(deleted));

        ApiResponse<com.sigi.presentation.dto.order.OrderDto> response = orderService.getDeletedOrderById(orderId);

        assertEquals(200, response.getCode());
        verify(orderRepository, times(1)).findByIdAndActiveFalse(orderId);
    }

    @Test
    void shouldThrowWhenDeletedOrderNotFound() {
        when(orderRepository.findByIdAndActiveFalse(orderId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> orderService.getDeletedOrderById(orderId));
    }

    // ------------------- list paginados -------------------
    @Test
    void shouldListAllActiveOrdersSuccessfully() {
        PagedRequestDto req = PagedRequestDto.builder()
                .page(0).size(10).sortDirection("desc").sortField("createdAt").build();

        Page<Order> page = new PageImpl<>(List.of(order), PageRequest.of(0, 10), 1);
        doReturn(page).when(orderRepository).findByActiveTrue(any(Pageable.class));

        Page<OrderDto> dtoPage = new PageImpl<>(
                List.of(OrderDto.builder().id(order.getId()).status("CONFIRMED").build()),
                PageRequest.of(0, 10),
                1
        );
        when(dtoMapper.toOrderDtoPage(any(Page.class))).thenReturn(dtoPage);

        ApiResponse<PagedResponse<OrderDto>> response = orderService.listAllActiveOrders(req);

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().getTotalElements());
        assertEquals(order.getId(), response.getData().getContent().get(0).getId());

        verify(orderRepository, times(1)).findByActiveTrue(any(Pageable.class));
        verify(dtoMapper, times(1)).toOrderDtoPage(page);
    }
    // ------------------- cancelOrder -------------------
    @Test
    void shouldCancelConfirmedOrderAndReleaseReservations() {
        Inventory inv = Inventory.builder().id(UUID.randomUUID()).build();
        OrderLine line = OrderLine.builder().inventory(inv).quantity(BigDecimal.valueOf(1)).build();
        order.setLines(List.of(line));
        order.setStatus(OrderStatus.CONFIRMED);

        when(persistenceMethod.getOrderById(orderId)).thenReturn(order);
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("canceler@example.com");
        when(orderRepository.save(order)).thenReturn(order);

        ApiResponse<Void> response = orderService.cancelOrder(orderId);

        assertEquals(200, response.getCode());
        assertEquals(OrderStatus.CANCELED, order.getStatus());
        verify(inventoryService, times(1)).releaseInventoryReservation(eq(inv.getId()), eq(line.getQuantity()), eq(order.getId()), eq(order.getUser().getId()), contains("Order canceled"));
        verify(notificationService, times(1)).createNotification(eq("Order Canceled"), contains("Order ID"), eq(order.getUser().getId()));
    }

    @Test
    void shouldCancelDraftOrderWithoutReleasing() {
        order.setStatus(OrderStatus.DRAFT);
        when(persistenceMethod.getOrderById(orderId)).thenReturn(order);
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("canceler@example.com");
        when(orderRepository.save(order)).thenReturn(order);

        ApiResponse<Void> response = orderService.cancelOrder(orderId);

        assertEquals(200, response.getCode());
        assertEquals(OrderStatus.CANCELED, order.getStatus());
        verify(inventoryService, never()).releaseInventoryReservation(any(), any(), any(), any(), anyString());
    }

    // ------------------- changeOrderStatus -------------------
    @Test
    void shouldConfirmOrderAndReserveStock() {
        Inventory inv = Inventory.builder().id(UUID.randomUUID()).build();
        OrderLine line = OrderLine.builder().inventory(inv).quantity(BigDecimal.valueOf(2)).product(Product.builder().id(UUID.randomUUID()).build()).build();
        order.setLines(List.of(line));
        order.setStatus(OrderStatus.DRAFT);

        when(persistenceMethod.getOrderById(orderId)).thenReturn(order);
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("changer@example.com");

        ApiResponse<Void> response = orderService.changeOrderStatus(orderId, "CONFIRMED");

        assertEquals(200, response.getCode());
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        verify(inventoryService, times(1)).reserveInventoryStock(eq(inv.getId()), eq(line.getQuantity()), eq(order.getId()), any(), contains("Reserve for order"));
    }

    @Test
    void shouldThrowWhenConfirmingNonDraftOrder() {
        order.setStatus(OrderStatus.CONFIRMED);
        when(persistenceMethod.getOrderById(orderId)).thenReturn(order);

        assertThrows(IllegalStateException.class, () -> orderService.changeOrderStatus(orderId, "CONFIRMED"));
    }

    @Test
    void shouldDeliverConfirmedOrderAndRegisterExit() {
        Inventory inv = Inventory.builder().id(UUID.randomUUID()).build();
        OrderLine line = OrderLine.builder().inventory(inv).quantity(BigDecimal.valueOf(1)).product(Product.builder().id(UUID.randomUUID()).build()).build();
        order.setLines(List.of(line));
        order.setStatus(OrderStatus.CONFIRMED);

        when(persistenceMethod.getOrderById(orderId)).thenReturn(order);
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("deliverer@example.com");

        ApiResponse<Void> response = orderService.changeOrderStatus(orderId, "DELIVERED");

        assertEquals(200, response.getCode());
        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        verify(inventoryService, times(1)).registerInventoryExit(order.getId());
    }

    @Test
    void shouldThrowWhenDeliveringNonConfirmedOrder() {
        order.setStatus(OrderStatus.DRAFT);
        when(persistenceMethod.getOrderById(orderId)).thenReturn(order);

        assertThrows(IllegalStateException.class, () -> orderService.changeOrderStatus(orderId, "DELIVERED"));
    }
}