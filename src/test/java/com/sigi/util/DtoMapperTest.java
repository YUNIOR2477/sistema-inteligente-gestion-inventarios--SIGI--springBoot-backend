package com.sigi.util;

import com.sigi.persistence.entity.*;
import com.sigi.persistence.enums.InvoiceStatus;
import com.sigi.persistence.enums.MovementType;
import com.sigi.persistence.enums.OrderStatus;
import com.sigi.persistence.enums.RoleList;
import com.sigi.presentation.dto.client.ClientDto;
import com.sigi.presentation.dto.dispatcher.DispatcherDto;
import com.sigi.presentation.dto.inventory.InventoryDto;
import com.sigi.presentation.dto.invoice.InvoiceDto;
import com.sigi.presentation.dto.movement.MovementDto;
import com.sigi.presentation.dto.order.OrderDto;
import com.sigi.presentation.dto.order.OrderLineDto;
import com.sigi.presentation.dto.product.ProductDto;
import com.sigi.presentation.dto.user.UserDto;
import com.sigi.presentation.dto.warehouse.WarehouseDto;
import com.sigi.presentation.dto.websocket.chatmessage.ChatMessageDto;
import com.sigi.presentation.dto.websocket.chatroom.ChatRoomDto;
import com.sigi.presentation.dto.websocket.notification.NotificationDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DtoMapperTest {

    private DtoMapper mapper;

    private final Role role = Role.builder().id(1L).name(RoleList.ROLE_ADMIN).build();

    private final Product product = Product.builder()
            .id(UUID.randomUUID())
            .sku("SKU-1")
            .name("Prod")
            .category("Cat")
            .unit("u")
            .price(BigDecimal.TEN)
            .barcode("123")
            .imageUrl("url")
            .active(true)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .deletedAt(null)
            .createdBy("me")
            .updatedBy("you")
            .deletedBy(null)
            .build();

   private final Client client = Client.builder()
            .id(UUID.randomUUID())
            .name("Client")
            .identification("ID")
            .location("Loc")
            .phone("123")
            .email("a@b.com")
            .active(true)
            .createdAt(LocalDateTime.now())
            .createdBy("me")
            .build();

   private final Warehouse warehouse = Warehouse.builder()
            .id(UUID.randomUUID())
            .name("W")
            .location("L")
            .totalCapacity(1000)
            .active(true)
            .createdAt(LocalDateTime.now())
            .createdBy("me")
            .updatedAt(LocalDateTime.now())
            .updatedBy("you")
            .deletedAt(null)
            .deletedBy(null)
            .build();

   private final Inventory inventory = Inventory.builder()
            .id(UUID.randomUUID())
            .product(product)
            .warehouse(warehouse)
            .location("Shelf")
            .lot("LOT")
            .active(true)
            .productionDate(LocalDate.now().minusDays(1))
            .expirationDate(LocalDate.now().plusDays(10))
            .availableQuantity(BigDecimal.valueOf(5))
            .reservedQuantity(BigDecimal.ZERO)
            .createdAt(LocalDateTime.now())
            .createdBy("me")
            .build();

   private final User user = User.builder()
            .id(UUID.randomUUID())
            .name("N")
            .surname("S")
            .phoneNumber("123")
            .email("e@e.com")
            .role(role)
            .active(true)
            .chatRooms(new ArrayList<>())
            .createdAt(LocalDateTime.now())
            .build();

   private final Order order = Order.builder()
            .id(UUID.randomUUID())
            .status(OrderStatus.CONFIRMED)
           .client(client)
            .dispatcher(Dispatcher.builder()
                    .id(UUID.randomUUID())
                    .name("D")
                    .contact("c")
                    .phone("p")
                    .orderList(new ArrayList<>())
                    .email("d@d.com")
                    .location("L")
                    .identification("ID")
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .build())
            .user(user)
            .warehouse(warehouse)
            .total(BigDecimal.valueOf(100))
            .lines(new ArrayList<>())
            .active(true)
            .createdAt(LocalDateTime.now())
            .build();

   private final Dispatcher dispatcher = Dispatcher.builder()
            .id(UUID.randomUUID())
            .name("D")
            .contact("c")
            .phone("p")
            .orderList(List.of(order))
            .email("d@d.com")
            .location("L")
            .identification("ID")
            .active(true)
            .createdAt(LocalDateTime.now())
            .build();

   private final OrderLine orderLine = OrderLine.builder()
            .id(UUID.randomUUID())
            .order(order)
            .product(product)
            .inventory(inventory)
            .lot("L")
            .quantity(BigDecimal.ONE)
            .unitPrice(BigDecimal.ONE)
            .active(true)
            .build();

  private final   Movement m = Movement.builder()
            .id(UUID.randomUUID())
            .type(MovementType.ENTRY)
            .inventory(inventory)
            .product(product)
            .user(user)
            .order(order)
            .dispatcher(dispatcher)
            .quantity(BigDecimal.ONE)
            .active(true)
            .createdAt(LocalDateTime.now())
            .build();

   private final Invoice invoice = Invoice.builder()
            .id(UUID.randomUUID())
            .number("INV-1")
            .client(client)
            .order(order)
            .subtotal(BigDecimal.ONE)
            .tax(BigDecimal.ZERO)
            .total(BigDecimal.ONE)
            .status(InvoiceStatus.CANCELED)
            .active(true)
            .createdAt(LocalDateTime.now())
            .build();

    @BeforeEach
    void setUp() {
        mapper = new DtoMapper();
    }


    // ---------- Product ----------
    @Test
    void toProductDto_mapsAllFields() {
        ProductDto dto = mapper.toProductDto(product);
        assertEquals(product.getId(), dto.getId());
        assertEquals("SKU-1", dto.getSku());
        assertEquals(BigDecimal.TEN, dto.getPrice());
        assertTrue(dto.getActive());
    }

    @Test
    void toProductDto_throwsOnNull() {
        assertThrows(IllegalArgumentException.class, () -> mapper.toProductDto(null));
    }

    @Test
    void toProductDtoPage_andList() {
        Product p = Product.builder().id(UUID.randomUUID()).name("P").sku("S").price(BigDecimal.ONE).active(true).build();
        Page<Product> page = new PageImpl<>(List.of(p), PageRequest.of(0, 10), 1);
        assertEquals(1, mapper.toProductDtoPage(page).getTotalElements());
        assertEquals(1, mapper.toProductDtoList(List.of(p)).size());
    }

    // ---------- Warehouse ----------
    @Test
    void toWarehouseDto_mapsAllFields() {
        WarehouseDto dto = mapper.toWarehouseDto(warehouse);
        assertEquals(warehouse.getId(), dto.getId());
        assertEquals("W", dto.getName());
        assertEquals(1000, dto.getTotalCapacity());
    }

    @Test
    void toWarehouseDto_throwsOnNull() {
        assertThrows(IllegalArgumentException.class, () -> mapper.toWarehouseDto(null));
    }

    @Test
    void toWarehouseDtoPage() {
        Page<Warehouse> page = new PageImpl<>(List.of(warehouse));
        assertEquals(1, mapper.toWarehouseDtoPage(page).getTotalElements());
    }

    // ---------- Inventory ----------
    @Test
    void toInventoryDto_mapsNestedProductAndWarehouse() {
        InventoryDto dto = mapper.toInventoryDto(inventory);
        assertEquals(inventory.getId(), dto.getId());
        assertEquals("Shelf", dto.getLocation());
        assertNotNull(dto.getProduct());
        assertNotNull(dto.getWarehouse());
    }

    @Test
    void toInventoryDto_throwsOnNull() {
        assertThrows(IllegalArgumentException.class, () -> mapper.toInventoryDto(null));
    }

    @Test
    void toInventoryDtoPage() {
        Page<Inventory> page = new PageImpl<>(List.of(inventory));
        assertEquals(1, mapper.toInventoryDtoPage(page).getTotalElements());
    }

    // ---------- Client ----------
    @Test
    void toClientDto_mapsFields() {
        ClientDto dto = mapper.toClientDto(client);
        assertEquals(client.getId(), dto.getId());
        assertEquals("Client", dto.getName());
    }

    @Test
    void toClientDto_throwsOnNull() {
        assertThrows(IllegalArgumentException.class, () -> mapper.toClientDto(null));
    }

    // ---------- Dispatcher ----------
    @Test
    void toDispatcherDto_returnsNullWhenNull() {
        assertNull(mapper.toDispatcherDto(null));
    }

    @Test
    void toDispatcherDto_mapsFieldsAndOrders() {
        DispatcherDto dto = mapper.toDispatcherDto(dispatcher);
        assertEquals(dispatcher.getId(), dto.getId());
        assertEquals(1, dto.getOrders().size());
    }

    @Test
    void toDispatcherDtoPage() {
        Page<Dispatcher> page = new PageImpl<>(List.of(dispatcher));
        assertEquals(1, mapper.toDispatcherDtoPage(page).getTotalElements());
    }

    // ---------- Movement ----------
    @Test
    void toMovementDto_mapsAllFields() {
        MovementDto dto = mapper.toMovementDto(m);
        assertEquals(m.getId(), dto.getId());
        assertEquals("ENTRY", dto.getType());
        assertNotNull(dto.getUser());
    }

    @Test
    void toMovementDto_throwsOnNull() {
        assertThrows(IllegalArgumentException.class, () -> mapper.toMovementDto(null));
    }

    @Test
    void toMovementDtoPage() {
        Page<Movement> page = new PageImpl<>(List.of(m));
        assertEquals(1, mapper.toMovementDtoPage(page).getTotalElements());
    }

    // ---------- Order ----------
    @Test
    void toOrderDto_mapsStatusAndLines() {
        OrderDto dto = mapper.toOrderDto(order);
        assertEquals(order.getId(), dto.getId());
        assertEquals("CONFIRMED", dto.getStatus());
        assertEquals(0, dto.getLines().size());
    }

    @Test
    void toOrderDto_throwsOnNull() {
        assertThrows(IllegalArgumentException.class, () -> mapper.toOrderDto(null));
    }

    @Test
    void toOrderDtoPage_andList() {
        Page<Order> page = new PageImpl<>(List.of(order));
        assertEquals(1, mapper.toOrderDtoPage(page).getTotalElements());
        assertEquals(1, mapper.toOrderDtoList(List.of(order)).size());
    }

    // ---------- Online OrderLine ----------
    @Test
    void toOnlineOrderDto_mapsFields() {
        OrderLineDto dto = mapper.toOnlineOrderDto(orderLine);
        assertEquals(orderLine.getId(), dto.getId());
        assertEquals(order.getId(), dto.getOrderId());
    }

    @Test
    void toOnlineOrderDto_throwsOnNull() {
        assertThrows(IllegalArgumentException.class, () -> mapper.toOnlineOrderDto(null));
    }

    @Test
    void toOnlineOrderDtoPage_andList() {
        Page<OrderLine> page = new PageImpl<>(List.of(orderLine));
        assertEquals(1, mapper.toOnlineOrderDtoPage(page).getTotalElements());
        assertEquals(1, mapper.toOnlineOrderDtoList(List.of(orderLine)).size());
    }

    // ---------- User ----------
    @Test
    void toUserDto_mapsFieldsAndChatRooms() {
        UserDto dto = mapper.toUserDto(user);
        assertEquals(user.getId(), dto.getId());
        assertEquals("e@e.com", dto.getEmail());
        assertEquals(0, dto.getChatRoomsIds().size());
    }

    @Test
    void toUserDto_throwsOnNull() {
        assertThrows(IllegalArgumentException.class, () -> mapper.toUserDto(null));
    }

    @Test
    void toUserDtoPage() {
        Page<User> page = new PageImpl<>(List.of(user));
        assertEquals(1, mapper.toUserDtoPage(page).getTotalElements());
    }

    // ---------- Invoice ----------
    @Test
    void toInvoiceDto_mapsFields() {
        InvoiceDto dto = mapper.toInvoiceDto(invoice);
        assertEquals(invoice.getId(), dto.getId());
        assertEquals("INV-1", dto.getNumber());
    }

    @Test
    void toInvoiceDto_throwsOnNull() {
        assertThrows(IllegalArgumentException.class, () -> mapper.toInvoiceDto(null));
    }

    @Test
    void toInvoiceDtoPage_mapsPageCorrectly() {
        Page<Invoice> page = new PageImpl<>(List.of(invoice), PageRequest.of(0, 10), 1);
        Page<InvoiceDto> dtoPage = mapper.toInvoiceDtoPage(page);
        assertNotNull(dtoPage);
        assertEquals(1, dtoPage.getTotalElements());
        assertEquals(invoice.getId(), dtoPage.getContent().get(0).getId());
        assertEquals(client.getId(), dtoPage.getContent().get(0).getClient().getId());
    }


    // ---------- Notification ----------
    @Test
    void toNotificationDto_mapsFields() {
        User u = User.builder().id(UUID.randomUUID()).email("u@u.com").build();
        Notification n = Notification.builder()
                .id(UUID.randomUUID())
                .title("T")
                .message("M")
                .isRead(false)
                .user(u)
                .createdAt(LocalDateTime.now())
                .build();

        NotificationDto dto = mapper.toNotificationDto(n);
        assertEquals(n.getId(), dto.getId());
        assertEquals("T", dto.getTitle());
        assertEquals("u@u.com", dto.getUser());
    }

    @Test
    void toNotificationDto_throwsOnNull() {
        assertThrows(IllegalArgumentException.class, () -> mapper.toNotificationDto(null));
    }

    @Test
    void toNotificationDtoPage() {
        User u = User.builder().id(UUID.randomUUID()).email("u@u.com").build();
        Notification n = Notification.builder()
                .id(UUID.randomUUID())
                .title("T")
                .message("M")
                .isRead(false)
                .user(u)
                .createdAt(LocalDateTime.now())
                .build();
        Page<Notification> page = new PageImpl<>(List.of(n));
        assertEquals(1, mapper.toNotificationDtoPage(page).getTotalElements());
    }

    // ---------- ChatMessage ----------
    @Test
    void toChatMessageDto_mapsFields() {
        User sender = User.builder().id(UUID.randomUUID()).email("s@e.com").build();
        ChatRoom room = ChatRoom.builder().id(UUID.randomUUID()).build();
        ChatMessage cm = ChatMessage.builder()
                .id(UUID.randomUUID())
                .room(room)
                .sender(sender)
                .content("hello")
                .isRead(false)
                .sentAt(LocalDateTime.now())
                .build();

        ChatMessageDto dto = mapper.toChatMessageDto(cm);
        assertEquals(cm.getId(), dto.getId());
        assertEquals("hello", dto.getContent());
        assertEquals(sender.getEmail(), dto.getSenderEmail());
    }

    @Test
    void toChatMessageDtoPage() {
        User sender = User.builder().id(UUID.randomUUID()).email("s@e.com").build();
        ChatRoom room = ChatRoom.builder().id(UUID.randomUUID()).build();
        ChatMessage cm = ChatMessage.builder()
                .id(UUID.randomUUID())
                .room(room)
                .sender(sender)
                .content("hello")
                .isRead(false)
                .sentAt(LocalDateTime.now())
                .build();
        Page<ChatMessage> page = new PageImpl<>(List.of(cm));
        assertEquals(1, mapper.toChatMessageDtoPage(page).getTotalElements());
    }

    // ---------- ChatRoom ----------
    @Test
    void toChatRoomDto_mapsFieldsAndUnreadCount() {
        User u1 = User.builder().id(UUID.randomUUID()).email("a@a.com").build();
        ChatMessage m1 = ChatMessage.builder().id(UUID.randomUUID()).isRead(false).build();
        ChatRoom cr = ChatRoom.builder()
                .id(UUID.randomUUID())
                .name("Room")
                .active(true)
                .participants(List.of(u1))
                .messages(List.of(m1))
                .createdAt(LocalDateTime.now())
                .build();

        ChatRoomDto dto = mapper.toChatRoomDto(cr);
        assertEquals(cr.getId(), dto.getId());
        assertEquals(1, dto.getParticipantEmails().size());
        assertEquals(1, dto.getUnread());
    }

    @Test
    void toChatRoomDtoPage() {
        User u1 = User.builder().id(UUID.randomUUID()).email("a@a.com").build();
        ChatMessage m1 = ChatMessage.builder().id(UUID.randomUUID()).isRead(false).build();
        ChatRoom cr = ChatRoom.builder()
                .id(UUID.randomUUID())
                .name("Room")
                .active(true)
                .participants(List.of(u1))
                .messages(List.of(m1))
                .createdAt(LocalDateTime.now())
                .build();
        Page<ChatRoom> page = new PageImpl<>(List.of(cr));
        assertEquals(1, mapper.toChatRoomDtoPage(page).getTotalElements());
    }
}