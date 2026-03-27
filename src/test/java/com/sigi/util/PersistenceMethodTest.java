package com.sigi.util;

import com.sigi.persistence.entity.*;
import com.sigi.persistence.enums.RoleList;
import com.sigi.persistence.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersistenceMethodTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private DispatcherRepository dispatcherRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderLineRepository onlineOrderRepository;
    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private MovementRepository movementRepository;

    private PersistenceMethod persistenceMethod;

    private User user;
    private Client client;
    private Dispatcher dispatcher;
    private Product product;
    private Warehouse warehouse;
    private Inventory inventory;
    private Order order;
    private OrderLine onlineOrder;
    private Invoice invoice;
    private Notification notification;
    private ChatRoom chatRoom;
    private Role role;
    private Movement movement;

    @BeforeEach
    void setUp() {
        persistenceMethod = new PersistenceMethod(userRepository, clientRepository, dispatcherRepository,
                productRepository, inventoryRepository, orderRepository, onlineOrderRepository,
                warehouseRepository,movementRepository, invoiceRepository, notificationRepository, chatRoomRepository, roleRepository);

        user = User.builder().id(UUID.randomUUID()).email("test@mail.com").build();
        client = Client.builder().id(UUID.randomUUID()).name("Juan").build();
        dispatcher = Dispatcher.builder().id(UUID.randomUUID()).name("DHL").build();
        product = Product.builder().id(UUID.randomUUID()).sku("SKU-123").build();
        warehouse = Warehouse.builder().id(UUID.randomUUID()).name("Central").build();
        inventory = Inventory.builder().id(UUID.randomUUID()).lot("LOT-1").build();
        order = Order.builder().id(UUID.randomUUID()).build();
        onlineOrder = OrderLine.builder().id(UUID.randomUUID()).build();
        invoice = Invoice.builder().id(UUID.randomUUID()).number("INV-001").build();
        notification = Notification.builder().id(UUID.randomUUID()).title("Test").build();
        chatRoom = ChatRoom.builder().id(UUID.randomUUID()).name("Room").build();
        role = Role.builder().id(1L).name(RoleList.ROLE_ADMIN).build();
        movement = Movement.builder().id(UUID.randomUUID()).build();
    }

    // ------------------- User -------------------
    @Test
    void shouldGetUserByIdSuccessfully() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        User result = persistenceMethod.getUserById(user.getId());
        assertEquals(user.getId(), result.getId());
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundById() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> persistenceMethod.getUserById(user.getId()));
    }

    @Test
    void shouldGetUserByEmailSuccessfully() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        User result = persistenceMethod.getUserByEmail(user.getEmail());
        assertEquals(user.getEmail(), result.getEmail());
    }

    @Test
    void shouldThrowExceptionWhenUserNotFoundByEmail() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> persistenceMethod.getUserByEmail(user.getEmail()));
    }

    // ------------------- Client -------------------
    @Test
    void shouldGetClientByIdSuccessfully() {
        when(clientRepository.findById(client.getId())).thenReturn(Optional.of(client));
        Client result = persistenceMethod.getClientById(client.getId());
        assertEquals(client.getId(), result.getId());
    }

    @Test
    void shouldThrowExceptionWhenClientNotFoundById() {
        when(clientRepository.findById(client.getId())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> persistenceMethod.getClientById(client.getId()));
    }

    @Test
    void shouldGetClientByIdentificationSuccessfully() {
        when(clientRepository.findByIdentificationAndActiveTrue("123")).thenReturn(Optional.of(client));
        Client result = persistenceMethod.getClientByIdentification("123");
        assertEquals(client.getId(), result.getId());
    }

    @Test
    void shouldThrowExceptionWhenClientNotFoundByIdentification() {
        when(clientRepository.findByIdentificationAndActiveTrue("123")).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> persistenceMethod.getClientByIdentification("123"));
    }

    // ------------------- Dispatcher -------------------
    @Test
    void shouldGetDispatcherByIdSuccessfully() {
        when(dispatcherRepository.findById(dispatcher.getId())).thenReturn(Optional.of(dispatcher));
        Dispatcher result = persistenceMethod.getDispatcherById(dispatcher.getId());
        assertEquals(dispatcher.getId(), result.getId());
    }

    @Test
    void shouldThrowExceptionWhenDispatcherNotFoundById() {
        when(dispatcherRepository.findById(dispatcher.getId())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> persistenceMethod.getDispatcherById(dispatcher.getId()));
    }

    // ------------------- Product -------------------
    @Test
    void shouldGetProductByIdSuccessfully() {
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        Product result = persistenceMethod.getProductById(product.getId());
        assertEquals(product.getId(), result.getId());
    }

    @Test
    void shouldThrowExceptionWhenProductNotFoundById() {
        when(productRepository.findById(product.getId())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> persistenceMethod.getProductById(product.getId()));
    }

    @Test
    void shouldGetProductBySkuSuccessfully() {
        when(productRepository.findBySkuAndActiveTrue(product.getSku())).thenReturn(Optional.of(product));
        Product result = persistenceMethod.getProductBySku(product.getSku());
        assertEquals(product.getSku(), result.getSku());
    }

    @Test
    void shouldThrowExceptionWhenProductNotFoundBySku() {
        when(productRepository.findBySkuAndActiveTrue(product.getSku())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> persistenceMethod.getProductBySku(product.getSku()));
    }

    // ------------------- Warehouse -------------------
    @Test
    void shouldGetWarehouseByIdSuccessfully() {
        when(warehouseRepository.findById(warehouse.getId())).thenReturn(Optional.of(warehouse));
        Warehouse result = persistenceMethod.getWarehouseById(warehouse.getId());
        assertEquals(warehouse.getId(), result.getId());
    }

    @Test
    void shouldThrowExceptionWhenWarehouseNotFoundById() {
        when(warehouseRepository.findById(warehouse.getId())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> persistenceMethod.getWarehouseById(warehouse.getId()));
    }

    @Test
    void shouldGetWarehouseByNameSuccessfully() {
        when(warehouseRepository.findByName(warehouse.getName())).thenReturn(Optional.of(warehouse));
        Warehouse result = persistenceMethod.getWarehouseByName(warehouse.getName());
        assertEquals(warehouse.getName(), result.getName());
    }

    @Test
    void shouldThrowExceptionWhenWarehouseNotFoundByName() {
        when(warehouseRepository.findByName(warehouse.getName())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> persistenceMethod.getWarehouseByName(warehouse.getName()));
    }

    // ------------------- Inventory -------------------
    @Test
    void shouldGetInventoryByIdSuccessfully() {
        when(inventoryRepository.findById(inventory.getId())).thenReturn(Optional.of(inventory));
        Inventory result = persistenceMethod.getInventoryById(inventory.getId());
        assertEquals(inventory.getId(), result.getId());
    }

    @Test
    void shouldThrowExceptionWhenInventoryNotFoundById() {
        when(inventoryRepository.findById(inventory.getId())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> persistenceMethod.getInventoryById(inventory.getId()));
    }

    // ------------------- Order -------------------
    @Test
    void shouldGetOrderByIdSuccessfully() {
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        Order result = persistenceMethod.getOrderById(order.getId());
        assertEquals(order.getId(), result.getId());
    }

    @Test
    void shouldThrowExceptionWhenOrderNotFoundById() {
        when(orderRepository.findById(order.getId())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> persistenceMethod.getOrderById(order.getId()));
    }

    // ------------------- OnlineOrder -------------------
    @Test
    void shouldGetOnlineOrderByIdSuccessfully() {
        when(onlineOrderRepository.findById(onlineOrder.getId())).thenReturn(Optional.of(onlineOrder));
        OrderLine result = persistenceMethod.getOnlineOrderById(onlineOrder.getId());
        assertEquals(onlineOrder.getId(), result.getId());
    }

    @Test
    void shouldThrowExceptionWhenOnlineOrderNotFoundById() {
        when(onlineOrderRepository.findById(onlineOrder.getId())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> persistenceMethod.getOnlineOrderById(onlineOrder.getId()));
    }

    // ------------------- Invoice -------------------
    @Test
    void shouldGetInvoiceByIdSuccessfully() {
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
        Invoice result = persistenceMethod.getInvoiceById(invoice.getId());
        assertEquals(invoice.getId(), result.getId());
    }

    @Test
    void shouldThrowExceptionWhenInvoiceNotFoundById() {
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> persistenceMethod.getInvoiceById(invoice.getId()));
    }

    @Test
    void shouldGetInvoiceByNumberSuccessfully() {
        when(invoiceRepository.findByNumberAndActiveTrue(invoice.getNumber())).thenReturn(Optional.of(invoice));
        Invoice result = persistenceMethod.getInvoiceByNumber(invoice.getNumber());
        assertEquals(invoice.getNumber(), result.getNumber());
    }

    @Test
    void shouldThrowExceptionWhenInvoiceNotFoundByNumber() {
        when(invoiceRepository.findByNumberAndActiveTrue(invoice.getNumber())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> persistenceMethod.getInvoiceByNumber(invoice.getNumber()));
    }

    // ------------------- Notification -------------------
    @Test
    void shouldGetNotificationByIdSuccessfully() {
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.of(notification));
        Notification result = persistenceMethod.getNotificationById(notification.getId());
        assertEquals(notification.getId(), result.getId());
    }

    @Test
    void shouldThrowExceptionWhenNotificationNotFoundById() {
        when(notificationRepository.findById(notification.getId())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> persistenceMethod.getNotificationById(notification.getId()));
    }

    // ------------------- ChatRoom -------------------
    @Test
    void shouldGetChatRoomByIdSuccessfully() {
        when(chatRoomRepository.findById(chatRoom.getId())).thenReturn(Optional.of(chatRoom));
        ChatRoom result = persistenceMethod.getChatRoomById(chatRoom.getId());
        assertEquals(chatRoom.getId(), result.getId());
    }

    @Test
    void shouldThrowExceptionWhenChatRoomNotFoundById() {
        when(chatRoomRepository.findById(chatRoom.getId())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> persistenceMethod.getChatRoomById(chatRoom.getId()));
    }

    // ------------------- getUsersByRoleName -------------------
    @Test
    void shouldGetUsersByRoleNameSuccessfully() {
        when(userRepository.findByRole_Name(RoleList.ROLE_ADMIN)).thenReturn(List.of(user));
        List<User> result = persistenceMethod.getUsersByRoleName(RoleList.ROLE_ADMIN);
        assertEquals(1, result.size());
        assertEquals(user.getId(), result.get(0).getId());
    }
}