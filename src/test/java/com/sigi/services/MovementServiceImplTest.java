package com.sigi.services;

import com.sigi.persistence.entity.*;
import com.sigi.persistence.enums.MovementType;
import com.sigi.persistence.enums.RoleList;
import com.sigi.persistence.repository.MovementRepository;
import com.sigi.presentation.dto.movement.MovementDto;
import com.sigi.presentation.dto.movement.NewMovementDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.services.service.websocket.notification.NotificationService;
import com.sigi.services.service.movement.MovementServiceImpl;
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
class MovementServiceImplTest {

    @Mock
    private MovementRepository movementRepository;

    @Mock
    private DtoMapper dtoMapper;

    @Mock
    private PersistenceMethod persistenceMethod;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private MovementServiceImpl movementService;

    private SimpleMeterRegistry meterRegistry;

    private UUID movementId;
    private UUID inventoryId;
    private UUID productId;
    private UUID orderId;
    private UUID dispatcherId;
    private User currentUser;
    private Product product;
    private Inventory inventory;
    private Movement movement;
    private MovementDto movementDto;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        movementService = new MovementServiceImpl(movementRepository, dtoMapper, meterRegistry, persistenceMethod, notificationService);

        movementId = UUID.randomUUID();
        inventoryId = UUID.randomUUID();
        productId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        dispatcherId = UUID.randomUUID();

        currentUser = User.builder().id(UUID.randomUUID()).build();

        product = Product.builder().id(productId).name("Prod X").build();
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

        movement = Movement.builder()
                .id(movementId)
                .type(MovementType.ENTRY)
                .inventory(inventory)
                .product(product)
                .quantity(BigDecimal.valueOf(5))
                .user(currentUser)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        movementDto = MovementDto.builder()
                .id(movementId)
                .type(MovementType.ENTRY.name())
                .quantity(BigDecimal.valueOf(5))
                .build();

        lenient().when(dtoMapper.toMovementDto(any(Movement.class))).thenReturn(movementDto);
        lenient().when(dtoMapper.toMovementDtoPage(any(Page.class))).thenAnswer(inv -> {
            Page<Movement> page = inv.getArgument(0);
            return page.map(m -> movementDto);
        });
    }

    // ------------------- createMovement -------------------
    @Test
    void shouldCreateMovementSuccessfully_whenInventoryProvided() {
        NewMovementDto dto = NewMovementDto.builder()
                .type(MovementType.ENTRY)
                .inventoryId(inventoryId)
                .productId(productId)
                .quantity(BigDecimal.valueOf(3))
                .motive("restock")
                .build();

        when(persistenceMethod.getCurrentUser()).thenReturn(currentUser);
        when(persistenceMethod.getInventoryById(inventoryId)).thenReturn(inventory);
        when(movementRepository.save(any(Movement.class))).thenAnswer(inv -> {
            Movement m = inv.getArgument(0);
            m.setId(movementId);
            return m;
        });
        when(persistenceMethod.getUsersByRoleName(RoleList.ROLE_DISPATCHER)).thenReturn(List.of());
        when(persistenceMethod.getUsersByRoleName(RoleList.ROLE_ADMIN)).thenReturn(List.of());
        when(persistenceMethod.getUsersByRoleName(RoleList.ROLE_WAREHOUSE)).thenReturn(List.of());

        ApiResponse<MovementDto> response = movementService.createMovement(dto);

        assertEquals(200, response.getCode());
        assertEquals(CREATED_SUCCESSFULLY.formatted(MOVEMENT), response.getMessage());
        verify(movementRepository, times(1)).save(any(Movement.class));
        verify(notificationService, atLeast(0)).createNotification(anyString(), anyString(), any(UUID.class));
        assertTrue(meterRegistry.get("movement.service.operations").tags("type", "createMovement").counter().count() >= 1.0);
    }

    @Test
    void shouldCreateMovementSuccessfully_whenProductProvidedAndNoInventory() {
        NewMovementDto dto = NewMovementDto.builder()
                .type(MovementType.ADJUSTMENT)
                .inventoryId(null)
                .productId(productId)
                .quantity(BigDecimal.valueOf(2))
                .build();

        when(persistenceMethod.getCurrentUser()).thenReturn(currentUser);
        when(persistenceMethod.getProductById(productId)).thenReturn(product);
        when(movementRepository.save(any(Movement.class))).thenReturn(movement);
        when(persistenceMethod.getUsersByRoleName(RoleList.ROLE_DISPATCHER)).thenReturn(List.of());
        when(persistenceMethod.getUsersByRoleName(RoleList.ROLE_ADMIN)).thenReturn(List.of());
        when(persistenceMethod.getUsersByRoleName(RoleList.ROLE_WAREHOUSE)).thenReturn(List.of());

        ApiResponse<MovementDto> response = movementService.createMovement(dto);

        assertEquals(200, response.getCode());
        assertEquals(CREATED_SUCCESSFULLY.formatted(MOVEMENT), response.getMessage());
        verify(movementRepository, times(1)).save(any(Movement.class));
    }

    @Test
    void shouldThrowWhenCreateMovementDtoNullOrMissingTypeOrQuantityInvalid() {
        assertThrows(IllegalArgumentException.class, () -> movementService.createMovement(null));

        NewMovementDto noType = NewMovementDto.builder().inventoryId(inventoryId).productId(productId).quantity(BigDecimal.ONE).build();
        noType.setType(null);
        assertThrows(IllegalArgumentException.class, () -> movementService.createMovement(noType));

        NewMovementDto zeroQty = NewMovementDto.builder().type(MovementType.ENTRY).inventoryId(inventoryId).productId(productId).quantity(BigDecimal.ZERO).build();
        assertThrows(IllegalArgumentException.class, () -> movementService.createMovement(zeroQty));
    }

    @Test
    void shouldThrowWhenCreateMovementInventoryInactiveOrProductMismatch() {
        NewMovementDto dto = NewMovementDto.builder()
                .type(MovementType.ENTRY)
                .inventoryId(inventoryId)
                .productId(productId)
                .quantity(BigDecimal.ONE)
                .build();

        Inventory inactive = Inventory.builder().id(inventoryId).active(false).build();
        when(persistenceMethod.getInventoryById(inventoryId)).thenReturn(inactive);
        assertThrows(IllegalArgumentException.class, () -> movementService.createMovement(dto));

        // product mismatch
        Inventory invWithOtherProduct = Inventory.builder().id(inventoryId).product(Product.builder().id(UUID.randomUUID()).build()).active(true).build();
        when(persistenceMethod.getInventoryById(inventoryId)).thenReturn(invWithOtherProduct);
        assertThrows(IllegalArgumentException.class, () -> movementService.createMovement(dto));
    }

    // ------------------- updateMovement -------------------
    @Test
    void shouldUpdateMovementSuccessfully_whenValid() {
        NewMovementDto dto = NewMovementDto.builder()
                .type(MovementType.EXIT)
                .inventoryId(inventoryId)
                .productId(productId)
                .quantity(BigDecimal.valueOf(4))
                .orderId(orderId)
                .dispatcherId(dispatcherId)
                .motive("deliver")
                .build();

        Movement existing = Movement.builder()
                .id(movementId)
                .type(MovementType.ENTRY)
                .inventory(inventory)
                .product(product)
                .quantity(BigDecimal.valueOf(1))
                .active(true)
                .build();

        when(persistenceMethod.getMovementById(movementId)).thenReturn(existing);
        when(persistenceMethod.getInventoryById(inventoryId)).thenReturn(inventory);
        when(persistenceMethod.getOrderById(orderId)).thenReturn(Order.builder().id(orderId).build());
        when(persistenceMethod.getDispatcherById(dispatcherId)).thenReturn(Dispatcher.builder().id(dispatcherId).build());
        when(persistenceMethod.getCurrentUser()).thenReturn(currentUser);
        when(movementRepository.save(existing)).thenReturn(existing);
        when(persistenceMethod.getUsersByRoleName(RoleList.ROLE_DISPATCHER)).thenReturn(List.of());
        when(persistenceMethod.getUsersByRoleName(RoleList.ROLE_ADMIN)).thenReturn(List.of());
        when(persistenceMethod.getUsersByRoleName(RoleList.ROLE_WAREHOUSE)).thenReturn(List.of());

        ApiResponse<MovementDto> response = movementService.updateMovement(movementId, dto);

        assertEquals(200, response.getCode());
        assertEquals(UPDATED_SUCCESSFULLY.formatted(MOVEMENT), response.getMessage());
        verify(movementRepository, times(1)).save(existing);
        assertEquals(MovementType.EXIT, existing.getType());
        assertEquals(BigDecimal.valueOf(4), existing.getQuantity());
    }

    @Test
    void shouldThrowWhenUpdateMovementInvalidCases() {
        // inactive movement
        Movement inactive = Movement.builder().id(movementId).active(false).build();
        when(persistenceMethod.getMovementById(movementId)).thenReturn(inactive);
        NewMovementDto dto = NewMovementDto.builder().type(MovementType.ENTRY).inventoryId(inventoryId).productId(productId).quantity(BigDecimal.ONE).build();
        assertThrows(IllegalArgumentException.class, () -> movementService.updateMovement(movementId, dto));

        // EXIT without dispatcher or order
        Movement active = Movement.builder().id(movementId).active(true).build();
        when(persistenceMethod.getMovementById(movementId)).thenReturn(active);
        NewMovementDto exitNoDispatcher = NewMovementDto.builder().type(MovementType.EXIT).inventoryId(inventoryId).productId(productId).quantity(BigDecimal.ONE).orderId(null).dispatcherId(null).build();
        assertThrows(IllegalArgumentException.class, () -> movementService.updateMovement(movementId, exitNoDispatcher));

        // inventory inactive
        NewMovementDto dtoInv = NewMovementDto.builder().type(MovementType.ENTRY).inventoryId(inventoryId).productId(productId).quantity(BigDecimal.ONE).build();
        Inventory inactiveInv = Inventory.builder().id(inventoryId).active(false).build();
        when(persistenceMethod.getInventoryById(inventoryId)).thenReturn(inactiveInv);
        assertThrows(IllegalArgumentException.class, () -> movementService.updateMovement(movementId, dtoInv));
    }

    // ------------------- deleteMovement / restoreMovement -------------------
    @Test
    void shouldDeleteMovementSuccessfully() {
        Movement existing = Movement.builder().id(movementId).active(true).build();
        when(persistenceMethod.getMovementById(movementId)).thenReturn(existing);
        when(movementRepository.save(existing)).thenReturn(existing);

        ApiResponse<Void> response = movementService.deleteMovement(movementId);

        assertEquals(200, response.getCode());
        assertEquals(DELETED_SUCCESSFULLY.formatted(MOVEMENT), response.getMessage());
        assertFalse(existing.getActive());
        assertNotNull(existing.getDeletedAt());
        verify(movementRepository, times(1)).save(existing);
    }

    @Test
    void shouldThrowWhenDeleteMovementAlreadyInactive() {
        Movement inactive = Movement.builder().id(movementId).active(false).build();
        when(persistenceMethod.getMovementById(movementId)).thenReturn(inactive);

        assertThrows(IllegalArgumentException.class, () -> movementService.deleteMovement(movementId));
        verify(movementRepository, never()).save(any());
    }

    @Test
    void shouldRestoreMovementSuccessfully() {
        Movement deleted = Movement.builder().id(movementId).active(false).build();
        when(persistenceMethod.getMovementById(movementId)).thenReturn(deleted);
        when(movementRepository.save(deleted)).thenReturn(deleted);

        ApiResponse<Void> response = movementService.restoreMovement(movementId);

        assertEquals(200, response.getCode());
        assertTrue(deleted.getActive());
        verify(movementRepository, times(1)).save(deleted);
    }

    @Test
    void shouldThrowWhenRestoreMovementAlreadyActive() {
        Movement active = Movement.builder().id(movementId).active(true).build();
        when(persistenceMethod.getMovementById(movementId)).thenReturn(active);

        assertThrows(IllegalArgumentException.class, () -> movementService.restoreMovement(movementId));
        verify(movementRepository, never()).save(any());
    }

    // ------------------- getMovementById / getDeletedMovementById -------------------
    @Test
    void shouldGetMovementByIdSuccessfullyWhenActive() {
        Movement active = Movement.builder().id(movementId).active(true).build();
        when(persistenceMethod.getMovementById(movementId)).thenReturn(active);

        ApiResponse<MovementDto> response = movementService.getMovementById(movementId);

        assertEquals(200, response.getCode());
        assertEquals(movementDto, response.getData());
    }

    @Test
    void shouldThrowWhenGetMovementByIdIfInactive() {
        Movement inactive = Movement.builder().id(movementId).active(false).build();
        when(persistenceMethod.getMovementById(movementId)).thenReturn(inactive);

        assertThrows(IllegalArgumentException.class, () -> movementService.getMovementById(movementId));
    }

    @Test
    void shouldGetDeletedMovementByIdSuccessfully() {
        Movement deleted = Movement.builder().id(movementId).active(false).build();
        when(movementRepository.findByIdAndActiveFalse(movementId)).thenReturn(Optional.of(deleted));

        ApiResponse<MovementDto> response = movementService.getDeletedMovementById(movementId);

        assertEquals(200, response.getCode());
        assertEquals(movementDto, response.getData());
    }

    @Test
    void shouldThrowWhenDeletedMovementNotFound() {
        when(movementRepository.findByIdAndActiveFalse(movementId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> movementService.getDeletedMovementById(movementId));
    }

    // ------------------- list paginados -------------------
    @Test
    void shouldListAllActiveMovementsSuccessfully() {
        PagedRequestDto req = PagedRequestDto.builder().page(0).size(10).sortDirection("desc").sortField("createdAt").build();
        Page<Movement> page = new PageImpl<>(List.of(movement), PageRequest.of(0,10), 1);
        when(movementRepository.findByActiveTrue(any(Pageable.class))).thenReturn(page);

        ApiResponse<PagedResponse<MovementDto>> response = movementService.listAllActiveMovements(req);

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().getTotalElements());
    }

    @Test
    void shouldListMovementsByTypeSuccessfully() {
        PagedRequestDto req = PagedRequestDto.builder().page(0).size(10).searchValue(MovementType.ENTRY.name()).sortDirection("desc").sortField("createdAt").build();
        Page<Movement> page = new PageImpl<>(List.of(movement));
        when(movementRepository.findByTypeAndActiveTrue(MovementType.ENTRY, PageRequest.of(0,10, Sort.by(Sort.Direction.DESC, "createdAt")))).thenReturn(page);

        ApiResponse<PagedResponse<MovementDto>> response = movementService.listMovementsByType(req);

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().getTotalElements());
    }
}