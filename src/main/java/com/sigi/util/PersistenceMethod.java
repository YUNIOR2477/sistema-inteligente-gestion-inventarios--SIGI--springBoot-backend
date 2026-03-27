package com.sigi.util;

import com.sigi.persistence.entity.*;
import com.sigi.persistence.enums.RoleList;
import com.sigi.persistence.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.sigi.util.Constants.*;

@Component
@RequiredArgsConstructor
public class PersistenceMethod {
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final DispatcherRepository dispatcherRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;
    private final OrderLineRepository onlineOrderRepository;
    private final WarehouseRepository warehouseRepository;
    private final MovementRepository movementRepository;
    private final InvoiceRepository invoiceRepository;
    private final NotificationRepository notificationRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final RoleRepository roleRepository;

    public User getCurrentUser() {
        String email = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(EMAIL_NOT_FOUND.formatted(email)));
    }

    public String getCurrentUserEmail() {
        return Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();
    }

    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ENTITY_NOT_FOUND.formatted(USER_ID.formatted(id))));
    }

    public Client getClientById(UUID id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ENTITY_NOT_FOUND.formatted(CLIENT_ID.formatted(id))));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException(ENTITY_NOT_FOUND.formatted(USER_EMAIL.formatted(email))));
    }

    public Dispatcher getDispatcherById(UUID id) {
        return dispatcherRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ENTITY_NOT_FOUND.formatted(DISPATCHER_ID.formatted(id))));
    }

    public Product getProductById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ENTITY_NOT_FOUND.formatted(PRODUCT_ID.formatted(id))));
    }

    public Warehouse getWarehouseById(UUID id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ENTITY_NOT_FOUND.formatted(WAREHOUSE_ID.formatted(id))));
    }

    public Inventory getInventoryById(UUID id) {
        return inventoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ENTITY_NOT_FOUND.formatted(INVENTORY_ID.formatted(id))));
    }

    public Order getOrderById(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ENTITY_NOT_FOUND.formatted(ORDER_ID.formatted(id))));

    }

    public OrderLine getOnlineOrderById(UUID id) {
        return onlineOrderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ENTITY_NOT_FOUND.formatted(ONLINE_ORDER_ID.formatted(id))));
    }

    public Product getProductBySku(String sku) {
        return productRepository.findBySkuAndActiveTrue(sku)
                .orElseThrow(() -> new EntityNotFoundException(ENTITY_NOT_FOUND.formatted(PRODUCT_SKU.formatted(sku))));
    }

    public Warehouse getWarehouseByName(String name) {
        return warehouseRepository.findByName(name)
                .orElseThrow(() -> new EntityNotFoundException(ENTITY_NOT_FOUND.formatted(WAREHOUSE_NAME.formatted(name))));
    }

    public Client getClientByIdentification(String identification) {
        return clientRepository.findByIdentificationAndActiveTrue(identification)
                .orElseThrow(() -> new EntityNotFoundException(ENTITY_NOT_FOUND.formatted(CLIENT_IDENTIFICATION.formatted(identification))));
    }

    public Invoice getInvoiceById(UUID id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ENTITY_NOT_FOUND.formatted(INVOICE_ID.formatted(id))));
    }

    public Invoice getInvoiceByNumber(String number) {
        return invoiceRepository.findByNumberAndActiveTrue(number)
                .orElseThrow(() -> new EntityNotFoundException(ENTITY_NOT_FOUND.formatted(INVOICE_NUMBER.formatted(number))));
    }

    public Notification getNotificationById(UUID id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ENTITY_NOT_FOUND.formatted(NOTIFICATION_ID.formatted(id))));
    }

    public ChatRoom getChatRoomById(UUID id) {
        return chatRoomRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ENTITY_NOT_FOUND.formatted(CHAT_ROOM_ID.formatted(id))));
    }

    public List<User> getUsersByRoleName(RoleList roleName) {
        return userRepository.findByRole_Name(roleName);
    }

    public Role getRoleByName(String roleName) {
        return roleRepository.findByName(RoleList.valueOf(roleName))
                .orElseThrow(() -> new EntityNotFoundException(ENTITY_NOT_FOUND.formatted(ROLE_NAME.formatted(roleName))));
    }

    public Movement getMovementById(UUID id) {
        return movementRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(ENTITY_NOT_FOUND.formatted(MOVEMENT_ID.formatted(id))));
    }
}
