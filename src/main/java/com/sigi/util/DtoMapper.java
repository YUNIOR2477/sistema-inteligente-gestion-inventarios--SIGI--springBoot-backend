package com.sigi.util;

import com.sigi.persistence.entity.*;
import com.sigi.presentation.dto.client.ClientDto;
import com.sigi.presentation.dto.dispatcher.DispatcherDto;
import com.sigi.presentation.dto.inventory.InventoryDto;
import com.sigi.presentation.dto.invoice.InvoiceDto;
import com.sigi.presentation.dto.movement.MovementDto;
import com.sigi.presentation.dto.order.OrderLineDto;
import com.sigi.presentation.dto.order.OrderDto;
import com.sigi.presentation.dto.product.ProductDto;
import com.sigi.presentation.dto.user.UserDto;
import com.sigi.presentation.dto.warehouse.WarehouseDto;
import com.sigi.presentation.dto.websocket.chatmessage.ChatMessageDto;
import com.sigi.presentation.dto.websocket.chatroom.ChatRoomDto;
import com.sigi.presentation.dto.websocket.notification.NotificationDto;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DtoMapper {

    public ProductDto toProductDto(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("The product cannot be null");
        }
        return ProductDto.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .category(product.getCategory())
                .unit(product.getUnit())
                .price(product.getPrice())
                .barcode(product.getBarcode())
                .imageUrl(product.getImageUrl())
                .active(product.getActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .deletedAt(product.getDeletedAt())
                .updatedBy(product.getUpdatedBy())
                .deletedBy(product.getDeletedBy())
                .createdBy(product.getCreatedBy())
                .build();
    }

    public Page<ProductDto> toProductDtoPage(Page<Product> products) {
        return products.map(this::toProductDto);
    }

    public List<ProductDto> toProductDtoList(List<Product> products) {
        return products.stream().map(this::toProductDto).toList();
    }


    public WarehouseDto toWarehouseDto(Warehouse warehouse) {
        if (warehouse == null) {
            throw new IllegalArgumentException("The warehouse cannot be null");
        }
        return WarehouseDto.builder()
                .id(warehouse.getId())
                .name(warehouse.getName())
                .location(warehouse.getLocation())
                .totalCapacity(warehouse.getTotalCapacity())
                .active(warehouse.getActive())
                .createdAt(warehouse.getCreatedAt())
                .createdBy(warehouse.getCreatedBy())
                .updatedAt(warehouse.getUpdatedAt())
                .updatedBy(warehouse.getUpdatedBy())
                .deletedAt(warehouse.getDeletedAt())
                .deletedBy(warehouse.getDeletedBy())
                .build();
    }

    public Page<WarehouseDto> toWarehouseDtoPage(Page<Warehouse> warehouses) {
        return warehouses.map(this::toWarehouseDto);
    }

    public InventoryDto toInventoryDto(Inventory inventory) {
        if (inventory == null) {
            throw new IllegalArgumentException("The inventory cannot be null");
        }
        return InventoryDto.builder()
                .id(inventory.getId())
                .product(toProductDto(inventory.getProduct()))
                .warehouse(toWarehouseDto(inventory.getWarehouse()))
                .location(inventory.getLocation())
                .lot(inventory.getLot())
                .active(inventory.getActive())
                .productionDate(inventory.getProductionDate())
                .expirationDate(inventory.getExpirationDate())
                .availableQuantity(inventory.getAvailableQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .createdAt(inventory.getCreatedAt())
                .createdBy(inventory.getCreatedBy())
                .updatedAt(inventory.getUpdatedAt())
                .updatedBy(inventory.getUpdatedBy())
                .deletedBy(inventory.getDeletedBy())
                .deletedAt(inventory.getDeletedAt())
                .build();
    }

    public Page<InventoryDto> toInventoryDtoPage(Page<Inventory> inventories) {
        return inventories.map(this::toInventoryDto);
    }

    public ClientDto toClientDto(Client client) {
        if (client == null) {
            throw new IllegalArgumentException("The client cannot be null");
        }
        return ClientDto.builder()
                .id(client.getId())
                .name(client.getName())
                .identification(client.getIdentification())
                .location(client.getLocation())
                .phone(client.getPhone())
                .email(client.getEmail())
                .active(client.getActive())
                .createdAt(client.getCreatedAt())
                .createdBy(client.getCreatedBy())
                .updatedAt(client.getUpdatedAt())
                .updatedBy(client.getUpdatedBy())
                .deletedBy(client.getDeletedBy())
                .deletedAt(client.getDeletedAt())
                .build();
    }

    public Page<ClientDto> toClientDtoPage(Page<Client> clients) {
        return clients.map(this::toClientDto);
    }

    public DispatcherDto toDispatcherDto(Dispatcher dispatcher) {
        if (dispatcher == null) {
            return null;
        }
        return DispatcherDto.builder()
                .id(dispatcher.getId())
                .name(dispatcher.getName())
                .contact(dispatcher.getContact())
                .phone(dispatcher.getPhone())
                .orders(toOrderDtoList(dispatcher.getOrderList()))
                .email(dispatcher.getEmail())
                .location(dispatcher.getLocation())
                .identification(dispatcher.getIdentification())
                .active(dispatcher.getActive())
                .createdAt(dispatcher.getCreatedAt())
                .createdBy(dispatcher.getCreatedBy())
                .updatedAt(dispatcher.getUpdatedAt())
                .updatedBy(dispatcher.getUpdatedBy())
                .deletedBy(dispatcher.getDeletedBy())
                .deletedAt(dispatcher.getDeletedAt())
                .build();
    }

    public Page<DispatcherDto> toDispatcherDtoPage(Page<Dispatcher> dispatchers) {
        return dispatchers.map(this::toDispatcherDto);
    }

    public MovementDto toMovementDto(Movement movement) {
        if (movement == null) {
            throw new IllegalArgumentException("The movement cannot be null");
        }
        return MovementDto.builder()
                .id(movement.getId())
                .type(movement.getType().name())
                .inventory(toInventoryDto(movement.getInventory()))
                .product(toProductDto(movement.getProduct()))
                .quantity(movement.getQuantity())
                .user(toUserDto(movement.getUser()))
                .order(movement.getOrder() == null ? null
                        : toOrderDto(movement.getOrder()))
                .dispatcher(toDispatcherDto(movement.getDispatcher()))
                .motive(movement.getMotive())
                .active(movement.getActive())
                .createdAt(movement.getCreatedAt())
                .createdBy(movement.getCreatedBy())
                .updatedAt(movement.getUpdatedAt())
                .updatedBy(movement.getUpdatedBy())
                .deletedBy(movement.getDeletedBy())
                .deletedAt(movement.getDeletedAt())
                .build();
    }

    public Page<MovementDto> toMovementDtoPage(Page<Movement> movements) {
        return movements.map(this::toMovementDto);
    }

    public OrderDto toOrderDto(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("The order cannot be null");
        }
        return OrderDto.builder()
                .id(order.getId())
                .client(toClientDto(order.getClient()))
                .user(toUserDto(order.getUser()))
                .warehouse(toWarehouseDto(order.getWarehouse()))
                .dispatcher(order.getDispatcher() == null ? null :
                        DispatcherDto.builder()
                                .id(order.getDispatcher().getId())
                                .name(order.getDispatcher().getName())
                                .build())
                .status(order.getStatus().name())
                .total(order.getTotal())
                .lines(order.getLines() == null ? List.of() :
                        order.getLines().stream()
                                .map(line -> OrderLineDto.builder()
                                        .id(line.getId())
                                        .inventory(toInventoryDto(line.getInventory()))
                                        .lot(line.getLot())
                                        .quantity(line.getQuantity())
                                        .unitPrice(line.getUnitPrice())
                                        .active(line.getActive())
                                        .build())
                                .toList())
                .status(order.getStatus().name())
                .total(order.getTotal())
                .lines(order.getLines() == null
                        ? List.of()
                        : order.getLines().stream()
                        .map(line -> OrderLineDto.builder()
                                .id(line.getId())
                                .inventory(toInventoryDto(line.getInventory()))
                                .lot(line.getLot())
                                .quantity(line.getQuantity())
                                .unitPrice(line.getUnitPrice())
                                .active(line.getActive())
                                .build())
                        .toList()
                )
                .active(order.getActive())
                .createdAt(order.getCreatedAt())
                .createdBy(order.getCreatedBy())
                .updatedAt(order.getUpdatedAt())
                .updatedBy(order.getUpdatedBy())
                .deletedBy(order.getDeletedBy())
                .deletedAt(order.getDeletedAt())
                .build();
    }

    public Page<OrderDto> toOrderDtoPage(Page<Order> orders) {
        return orders.map(this::toOrderDto);
    }

    public List<OrderDto> toOrderDtoList(List<Order> orders) {
        return orders.stream().map(this::toOrderDto).toList();
    }

    public OrderLineDto toOnlineOrderDto(OrderLine onlineOrder) {
        if (onlineOrder == null) {
            throw new IllegalArgumentException("The onlineOrder cannot be null");
        }
        return OrderLineDto.builder()
                .id(onlineOrder.getId())
                .orderId(onlineOrder.getOrder().getId())
                .inventory(toInventoryDto(onlineOrder.getInventory()))
                .lot(onlineOrder.getLot())
                .quantity(onlineOrder.getQuantity())
                .unitPrice(onlineOrder.getUnitPrice())
                .active(onlineOrder.getActive())
                .createdAt(onlineOrder.getCreatedAt())
                .createdBy(onlineOrder.getCreatedBy())
                .updatedAt(onlineOrder.getUpdatedAt())
                .updatedBy(onlineOrder.getUpdatedBy())
                .deletedBy(onlineOrder.getDeletedBy())
                .deletedAt(onlineOrder.getDeletedAt())
                .build();
    }

    public Page<OrderLineDto> toOnlineOrderDtoPage(Page<OrderLine> onlineOrders) {
        return onlineOrders.map(this::toOnlineOrderDto);
    }

    public List<OrderLineDto> toOnlineOrderDtoList(List<OrderLine> onlineOrders) {
        return onlineOrders.stream().map(this::toOnlineOrderDto).toList();
    }

    public UserDto toUserDto(User user) {
        if (user == null) {
            throw new IllegalArgumentException("The user cannot be null");
        }
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .surname(user.getSurname())
                .phoneNumber(user.getPhoneNumber())
                .email(user.getEmail())
                .role(user.getRole().getName().toString())
                .active(user.getActive())
                .chatNotificationsEnabled(user.isChatNotificationsEnabled())
                .notificationsEnabled(user.isNotificationsEnabled())
                .chatRoomsIds(user.getChatRooms().stream().map(ChatRoom::getId).toList())
                .createdAt(user.getCreatedAt())
                .createdBy(user.getCreatedBy())
                .updatedAt(user.getUpdatedAt())
                .updatedBy(user.getUpdatedBy())
                .deletedBy(user.getDeletedBy())
                .deletedAt(user.getDeletedAt())
                .build();
    }

    public Page<UserDto> toUserDtoPage(Page<User> users) {
        return users.map(this::toUserDto);
    }

    public InvoiceDto toInvoiceDto(Invoice invoice) {
        if (invoice == null) {
            throw new IllegalArgumentException("The invoice cannot be null");
        }
        return InvoiceDto.builder()
                .id(invoice.getId())
                .number(invoice.getNumber())
                .client(toClientDto(invoice.getClient()))
                .order(toOrderDto(invoice.getOrder()))
                .subtotal(invoice.getSubtotal())
                .tax(invoice.getTax())
                .total(invoice.getTotal())
                .status(invoice.getStatus())
                .active(invoice.getActive())
                .createdAt(invoice.getCreatedAt())
                .createdBy(invoice.getCreatedBy())
                .updatedAt(invoice.getUpdatedAt())
                .updatedBy(invoice.getUpdatedBy())
                .build();
    }

    public Page<InvoiceDto> toInvoiceDtoPage(Page<Invoice> invoice) {
        return invoice.map(this::toInvoiceDto);
    }

    public NotificationDto toNotificationDto(Notification notification) {
        if (notification == null) {
            throw new IllegalArgumentException("The notification cannot be null");
        }

        return NotificationDto.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .user(notification.getUser().getEmail())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    public Page<NotificationDto> toNotificationDtoPage(Page<Notification> notifications) {
        return notifications.map(this::toNotificationDto);
    }

    public ChatMessageDto toChatMessageDto(ChatMessage chatMessage) {
        return ChatMessageDto.builder()
                .id(chatMessage.getId())
                .roomId(chatMessage.getRoom().getId())
                .senderEmail(chatMessage.getSender().getEmail())
                .content(chatMessage.getContent())
                .isRead(chatMessage.getIsRead())
                .sentAt(chatMessage.getSentAt())
                .build();
    }

    public Page<ChatMessageDto> toChatMessageDtoPage(Page<ChatMessage> chatMessages) {
        return chatMessages.map(this::toChatMessageDto);
    }

    public ChatRoomDto toChatRoomDto(ChatRoom chatRoom) {
        return ChatRoomDto.builder()
                .id(chatRoom.getId())
                .name(chatRoom.getName())
                .active(chatRoom.getActive())
                .participantEmails(chatRoom.getParticipants().stream().map(User::getEmail).toList())
                .participantIds(chatRoom.getParticipants().stream().map(User::getId).toList())
                .createdAt(chatRoom.getCreatedAt())
                .updatedAt(chatRoom.getUpdatedAt())
                .unread(chatRoom.getMessages() == null ? 0 : (int) chatRoom.getMessages().stream().filter(chatMessage -> chatMessage.getIsRead().equals(false)).count())
                .build();
    }

    public Page<ChatRoomDto> toChatRoomDtoPage(Page<ChatRoom> chatRooms) {
        return chatRooms.map(this::toChatRoomDto);
    }
}

