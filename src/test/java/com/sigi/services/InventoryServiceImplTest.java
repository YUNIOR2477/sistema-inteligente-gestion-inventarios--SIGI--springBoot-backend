package com.sigi.services;

import com.sigi.persistence.entity.Inventory;
import com.sigi.persistence.entity.Product;
import com.sigi.persistence.entity.User;
import com.sigi.persistence.entity.Warehouse;
import com.sigi.persistence.enums.RoleList;
import com.sigi.persistence.repository.InventoryRepository;
import com.sigi.presentation.dto.inventory.InventoryDto;
import com.sigi.presentation.dto.inventory.NewEntryDto;
import com.sigi.presentation.dto.inventory.NewInventoryDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.services.service.inventory.InventoryServiceImpl;
import com.sigi.services.service.movement.MovementService;
import com.sigi.services.service.warehouse.WarehouseService;
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
import org.springframework.dao.OptimisticLockingFailureException;
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
class InventoryServiceImplTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private WarehouseService warehouseService;

    @Mock
    private MovementService movementService;

    @Mock
    private PersistenceMethod persistenceMethod;

    @Mock
    private DtoMapper dtoMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private InventoryServiceImpl inventoryService;

    private SimpleMeterRegistry meterRegistry;

    private UUID inventoryId;
    private Product product;
    private Warehouse warehouse;
    private Inventory inventory;
    private InventoryDto inventoryDto;
    private NewInventoryDto newInventoryDto;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        inventoryService = new InventoryServiceImpl(inventoryRepository, warehouseService, movementService, persistenceMethod, dtoMapper, meterRegistry, notificationService);

        inventoryId = UUID.randomUUID();
        product = Product.builder().id(UUID.randomUUID()).name("Prod A").build();
        warehouse = Warehouse.builder().id(UUID.randomUUID()).name("Main WH").build();

        inventory = Inventory.builder()
                .id(inventoryId)
                .product(product)
                .warehouse(warehouse)
                .lot("LOT-1")
                .availableQuantity(BigDecimal.ZERO)
                .reservedQuantity(BigDecimal.ZERO)
                .active(true)
                .createdAt(LocalDateTime.now())
                .createdBy("system")
                .build();

        inventoryDto = InventoryDto.builder()
                .id(inventoryId)
                .lot(inventory.getLot())
                .availableQuantity(inventory.getAvailableQuantity())
                .reservedQuantity(inventory.getReservedQuantity())
                .build();

        newInventoryDto = NewInventoryDto.builder()
                .productId(product.getId())
                .warehouseId(warehouse.getId())
                .lot("LOT-1")
                .location("A1")
                .availableQuantity(BigDecimal.TEN)
                .reservedQuantity(BigDecimal.ZERO)
                .build();

        lenient().when(dtoMapper.toInventoryDto(any(Inventory.class))).thenReturn(inventoryDto);
        lenient().when(dtoMapper.toInventoryDtoPage(any(Page.class))).thenAnswer(inv -> {
            Page<Inventory> page = inv.getArgument(0);
            return page.map(i -> inventoryDto);
        });
    }

    // ------------------- createInventory -------------------
    @Test
    void shouldCreateInventorySuccessfullyAndNotify() {
        when(persistenceMethod.getProductById(product.getId())).thenReturn(product);
        when(persistenceMethod.getWarehouseById(warehouse.getId())).thenReturn(warehouse);
        when(inventoryRepository.existsByProductIdAndWarehouseIdAndLotAndActiveTrue(product.getId(), warehouse.getId(), "LOT-1")).thenReturn(false);
        when(persistenceMethod.getUsersByRoleName(RoleList.ROLE_ADMIN)).thenReturn(List.of(User.builder().id(UUID.randomUUID()).build()));
        when(persistenceMethod.getUsersByRoleName(RoleList.ROLE_WAREHOUSE)).thenReturn(List.of(User.builder().id(UUID.randomUUID()).build()));
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("creator@example.com");
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> {
            Inventory i = inv.getArgument(0);
            i.setId(inventoryId);
            return i;
        });

        ApiResponse<InventoryDto> response = inventoryService.createInventory(newInventoryDto);

        assertEquals(200, response.getCode());
        assertEquals(CREATED_SUCCESSFULLY.formatted(INVENTORY), response.getMessage());
        verify(inventoryRepository, times(1)).save(any(Inventory.class));
        verify(notificationService, atLeastOnce()).createNotification(anyString(), contains("Lot: LOT-1"), any(UUID.class));
        assertTrue(meterRegistry.get("inventory.service.operations").tags("type", "createInventory").counter().count() >= 1.0);
    }

    @Test
    void shouldThrowWhenCreatingDuplicateInventoryByExistsCheck() {
        when(persistenceMethod.getProductById(product.getId())).thenReturn(product);
        when(persistenceMethod.getWarehouseById(warehouse.getId())).thenReturn(warehouse);
        when(inventoryRepository.existsByProductIdAndWarehouseIdAndLotAndActiveTrue(product.getId(), warehouse.getId(), "LOT-1")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> inventoryService.createInventory(newInventoryDto));
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenCreateInventoryDataIntegrityViolation() {
        when(persistenceMethod.getProductById(product.getId())).thenReturn(product);
        when(persistenceMethod.getWarehouseById(warehouse.getId())).thenReturn(warehouse);
        when(inventoryRepository.existsByProductIdAndWarehouseIdAndLotAndActiveTrue(product.getId(), warehouse.getId(), "LOT-1")).thenReturn(false);
        when(inventoryRepository.save(any(Inventory.class))).thenThrow(new org.springframework.dao.DataIntegrityViolationException("dup"));

        assertThrows(IllegalArgumentException.class, () -> inventoryService.createInventory(newInventoryDto));
    }

    // ------------------- updateInventory -------------------
    @Test
    void shouldUpdateInventorySuccessfully_whenNoIdentityChange() {
        NewInventoryDto update = NewInventoryDto.builder()
                .productId(product.getId())
                .warehouseId(warehouse.getId())
                .lot("LOT-1")
                .location("B2")
                .availableQuantity(BigDecimal.valueOf(5))
                .reservedQuantity(BigDecimal.valueOf(2))
                .build();

        Inventory existing = Inventory.builder()
                .id(inventoryId)
                .product(product)
                .warehouse(warehouse)
                .lot("LOT-1")
                .availableQuantity(BigDecimal.TEN)
                .reservedQuantity(BigDecimal.ONE)
                .active(true)
                .build();

        when(persistenceMethod.getInventoryById(inventoryId)).thenReturn(existing);
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("updater@example.com");
        when(inventoryRepository.save(existing)).thenReturn(existing);
        when(persistenceMethod.getUsersByRoleName(RoleList.ROLE_ADMIN)).thenReturn(List.of(User.builder().id(UUID.randomUUID()).build()));
        when(persistenceMethod.getUsersByRoleName(RoleList.ROLE_WAREHOUSE)).thenReturn(List.of(User.builder().id(UUID.randomUUID()).build()));

        ApiResponse<InventoryDto> response = inventoryService.updateInventory(inventoryId, update);

        assertEquals(200, response.getCode());
        assertEquals(UPDATED_SUCCESSFULLY.formatted(INVENTORY), response.getMessage());
        assertEquals("B2", existing.getLocation());
        assertEquals(BigDecimal.valueOf(5), existing.getAvailableQuantity());
        verify(notificationService, atLeastOnce()).createNotification(anyString(), anyString(), any(UUID.class));
    }

    @Test
    void shouldThrowWhenUpdateInventoryReservedGreaterThanAvailable() {
        NewInventoryDto update = NewInventoryDto.builder()
                .productId(product.getId())
                .warehouseId(warehouse.getId())
                .lot("LOT-1")
                .availableQuantity(BigDecimal.valueOf(5))
                .reservedQuantity(BigDecimal.valueOf(10))
                .build();

        Inventory existing = Inventory.builder()
                .id(inventoryId)
                .product(product)
                .warehouse(warehouse)
                .lot("LOT-1")
                .availableQuantity(BigDecimal.TEN)
                .reservedQuantity(BigDecimal.ZERO)
                .active(true)
                .build();

        when(persistenceMethod.getInventoryById(inventoryId)).thenReturn(existing);

        assertThrows(IllegalArgumentException.class, () -> inventoryService.updateInventory(inventoryId, update));
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenUpdateInventoryNegativeAvailableOrReserved() {
        NewInventoryDto update = NewInventoryDto.builder()
                .productId(product.getId())
                .warehouseId(warehouse.getId())
                .lot("LOT-1")
                .availableQuantity(BigDecimal.valueOf(-1))
                .reservedQuantity(BigDecimal.ZERO)
                .build();

        Inventory existing = Inventory.builder()
                .id(inventoryId)
                .product(product)
                .warehouse(warehouse)
                .lot("LOT-1")
                .availableQuantity(BigDecimal.TEN)
                .reservedQuantity(BigDecimal.ZERO)
                .active(true)
                .build();

        when(persistenceMethod.getInventoryById(inventoryId)).thenReturn(existing);

        assertThrows(IllegalArgumentException.class, () -> inventoryService.updateInventory(inventoryId, update));
    }

    // ------------------- deleteInventory -------------------
    @Test
    void shouldDeleteInventorySuccessfullyWhenNoStock() {
        Inventory existing = Inventory.builder()
                .id(inventoryId)
                .product(product)
                .warehouse(warehouse)
                .lot("LOT-1")
                .availableQuantity(BigDecimal.ZERO)
                .reservedQuantity(BigDecimal.ZERO)
                .active(true)
                .build();

        when(persistenceMethod.getInventoryById(inventoryId)).thenReturn(existing);
        when(persistenceMethod.getUsersByRoleName(RoleList.ROLE_ADMIN)).thenReturn(List.of(User.builder().id(UUID.randomUUID()).build()));
        when(persistenceMethod.getUsersByRoleName(RoleList.ROLE_WAREHOUSE)).thenReturn(List.of(User.builder().id(UUID.randomUUID()).build()));
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("deleter@example.com");
        when(inventoryRepository.save(existing)).thenReturn(existing);

        ApiResponse<Void> response = inventoryService.deleteInventory(inventoryId);

        assertEquals(200, response.getCode());
        assertEquals(DELETED_SUCCESSFULLY.formatted(INVENTORY), response.getMessage());
        assertFalse(existing.getActive());
        assertNotNull(existing.getDeletedAt());
        verify(notificationService, atLeastOnce()).createNotification(anyString(), anyString(), any(UUID.class));
    }

    @Test
    void shouldThrowWhenDeletingInventoryWithAvailableOrReservedStock() {
        Inventory existing = Inventory.builder()
                .id(inventoryId)
                .availableQuantity(BigDecimal.ONE)
                .reservedQuantity(BigDecimal.ZERO)
                .active(true)
                .build();

        when(persistenceMethod.getInventoryById(inventoryId)).thenReturn(existing);

        assertThrows(IllegalArgumentException.class, () -> inventoryService.deleteInventory(inventoryId));
        verify(inventoryRepository, never()).save(any());
    }

    // ------------------- restoreInventory -------------------
    @Test
    void shouldRestoreInventorySuccessfullyWhenNoConflictAndCapacityOk() {
        Inventory inv = Inventory.builder()
                .id(inventoryId)
                .product(product)
                .warehouse(warehouse)
                .lot("LOT-1")
                .availableQuantity(BigDecimal.valueOf(2))
                .reservedQuantity(BigDecimal.valueOf(1))
                .active(false)
                .build();

        when(persistenceMethod.getInventoryById(inventoryId)).thenReturn(inv);
        when(inventoryRepository.existsByProductIdAndWarehouseIdAndLotAndActiveTrue(product.getId(), warehouse.getId(), "LOT-1")).thenReturn(false);
        when(warehouseService.validateAvailableCapacity(warehouse.getId(), BigDecimal.valueOf(3))).thenReturn(ApiResponse.success("ok", true));
        when(persistenceMethod.getUsersByRoleName(RoleList.ROLE_ADMIN)).thenReturn(List.of(User.builder().id(UUID.randomUUID()).build()));
        when(persistenceMethod.getUsersByRoleName(RoleList.ROLE_WAREHOUSE)).thenReturn(List.of(User.builder().id(UUID.randomUUID()).build()));
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("restorer@example.com");

        ApiResponse<Void> response = inventoryService.restoreInventory(inventoryId);

        assertEquals(200, response.getCode());
        assertTrue(inv.getActive());
        assertNull(inv.getDeletedAt());
        verify(notificationService, atLeastOnce()).createNotification(anyString(), anyString(), any(UUID.class));
    }

    @Test
    void shouldThrowWhenRestoreInventoryConflictExists() {
        Inventory inv = Inventory.builder().id(inventoryId).product(product).warehouse(warehouse).lot("LOT-1").active(false).build();
        when(persistenceMethod.getInventoryById(inventoryId)).thenReturn(inv);
        when(inventoryRepository.existsByProductIdAndWarehouseIdAndLotAndActiveTrue(product.getId(), warehouse.getId(), "LOT-1")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> inventoryService.restoreInventory(inventoryId));
    }

    @Test
    void shouldThrowWhenRestoreInventoryCapacityExceeded() {
        Inventory inv = Inventory.builder().id(inventoryId).product(product).warehouse(warehouse).lot("LOT-1").availableQuantity(BigDecimal.ONE).reservedQuantity(BigDecimal.ONE).active(false).build();
        when(persistenceMethod.getInventoryById(inventoryId)).thenReturn(inv);
        when(inventoryRepository.existsByProductIdAndWarehouseIdAndLotAndActiveTrue(product.getId(), warehouse.getId(), "LOT-1")).thenReturn(false);
        when(warehouseService.validateAvailableCapacity(warehouse.getId(), BigDecimal.valueOf(2))).thenReturn(ApiResponse.success("ok", false));

        assertThrows(IllegalStateException.class, () -> inventoryService.restoreInventory(inventoryId));
    }

    // ------------------- registerInventoryEntry -------------------
    @Test
    void shouldRegisterInventoryEntrySuccessfullyAndCreateMovementAndNotify() {
        UUID dispatcherId = UUID.randomUUID();
        NewEntryDto entry = NewEntryDto.builder()
                .inventoryId(inventoryId)
                .quantity(BigDecimal.valueOf(5))
                .dispatcherId(dispatcherId)
                .motive("restock")
                .build();

        Inventory inv = Inventory.builder()
                .id(inventoryId)
                .product(product)
                .warehouse(warehouse)
                .lot("LOT-1")
                .availableQuantity(BigDecimal.TEN)
                .reservedQuantity(BigDecimal.ZERO)
                .active(true)
                .build();

        when(persistenceMethod.getInventoryById(inventoryId)).thenReturn(inv);
        when(warehouseService.validateAvailableCapacity(warehouse.getId(), entry.getQuantity())).thenReturn(ApiResponse.success("ok", true));
        when(persistenceMethod.getUsersByRoleName(RoleList.ROLE_AUDITOR)).thenReturn(List.of(User.builder().id(UUID.randomUUID()).build()));
        when(persistenceMethod.getUsersByRoleName(RoleList.ROLE_ADMIN)).thenReturn(List.of(User.builder().id(UUID.randomUUID()).build()));
        when(persistenceMethod.getUsersByRoleName(RoleList.ROLE_WAREHOUSE)).thenReturn(List.of(User.builder().id(UUID.randomUUID()).build()));
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("operator@example.com");

        ApiResponse<Void> response = inventoryService.registerInventoryEntry(entry);

        assertEquals(200, response.getCode());
        assertEquals(OPERATION_COMPLETED_SUCCESSFULLY.formatted("registered entry"), response.getMessage());
        assertEquals(BigDecimal.valueOf(15), inv.getAvailableQuantity());
        verify(movementService, times(1)).createMovement(any());
        verify(notificationService, atLeastOnce()).createNotification(eq("Entry registered"), anyString(), any(UUID.class));
    }

    @Test
    void shouldThrowWhenRegisterEntryQuantityInvalid() {
        NewEntryDto entry = NewEntryDto.builder().inventoryId(inventoryId).quantity(BigDecimal.ZERO).build();
        assertThrows(IllegalArgumentException.class, () -> inventoryService.registerInventoryEntry(entry));
    }

    @Test
    void shouldRetryOnOptimisticLockingFailureAndSucceed() {
        NewEntryDto entry = NewEntryDto.builder()
                .inventoryId(inventoryId)
                .quantity(BigDecimal.valueOf(1))
                .dispatcherId(UUID.randomUUID())
                .motive("m")
                .build();

        Inventory inv = Inventory.builder()
                .id(inventoryId)
                .product(product)
                .warehouse(warehouse)
                .lot("LOT-1")
                .availableQuantity(BigDecimal.ZERO)
                .reservedQuantity(BigDecimal.ZERO)
                .active(true)
                .build();

        when(persistenceMethod.getInventoryById(inventoryId)).thenReturn(inv);
        doReturn(ApiResponse.success("ok", true))
                .when(warehouseService).validateAvailableCapacity(warehouse.getId(), entry.getQuantity());
        when(movementService.createMovement(any())).thenReturn(ApiResponse.success("ok", null));

        ApiResponse<Void> response = inventoryService.registerInventoryEntry(entry);

        assertEquals(200, response.getCode());
        verify(inventoryRepository, atLeast(1)).save(any(Inventory.class));
    }

    @Test
    void shouldFailAfterMaxRetriesOnOptimisticLockingFailure() {
        NewEntryDto entry = NewEntryDto.builder()
                .inventoryId(inventoryId)
                .quantity(BigDecimal.valueOf(1))
                .dispatcherId(UUID.randomUUID())
                .motive("m")
                .build();

        Inventory inv = Inventory.builder()
                .id(inventoryId)
                .product(product)
                .warehouse(warehouse)
                .lot("LOT-1")
                .availableQuantity(BigDecimal.ZERO)
                .reservedQuantity(BigDecimal.ZERO)
                .active(true)
                .build();

        when(persistenceMethod.getInventoryById(inventoryId)).thenReturn(inv);
        when(warehouseService.validateAvailableCapacity(warehouse.getId(), entry.getQuantity())).thenReturn(ApiResponse.success("ok", true));
        // always throw optimistic lock
        doThrow(new OptimisticLockingFailureException("optimistic")).when(inventoryRepository).save(any(Inventory.class));
        when(inventoryRepository.findById(inventory.getId())).thenReturn(Optional.of(inv));

        assertThrows(OptimisticLockingFailureException.class, () -> inventoryService.registerInventoryEntry(entry));
    }

    // ------------------- getInventoryById / getDeletedInventoryById / list paginados -------------------
    @Test
    void shouldGetInventoryByIdSuccessfully() {
        Inventory existing = Inventory.builder().id(inventoryId).active(true).build();
        when(persistenceMethod.getInventoryById(inventoryId)).thenReturn(existing);

        ApiResponse<InventoryDto> response = inventoryService.getInventoryById(inventoryId);

        assertEquals(200, response.getCode());
        assertEquals(inventoryDto, response.getData());
    }

    @Test
    void shouldThrowWhenGettingInactiveInventoryById() {
        Inventory inactive = Inventory.builder().id(inventoryId).active(false).build();
        when(persistenceMethod.getInventoryById(inventoryId)).thenReturn(inactive);

        assertThrows(IllegalArgumentException.class, () -> inventoryService.getInventoryById(inventoryId));
    }

    @Test
    void shouldGetDeletedInventoryByIdSuccessfully() {
        Inventory deleted = Inventory.builder().id(inventoryId).active(false).build();
        when(inventoryRepository.findByIdAndActiveFalse(inventoryId)).thenReturn(Optional.of(deleted));

        ApiResponse<InventoryDto> response = inventoryService.getDeletedInventoryById(inventoryId);

        assertEquals(200, response.getCode());
        assertEquals(inventoryDto, response.getData());
    }

    @Test
    void shouldThrowWhenDeletedInventoryNotFound() {
        when(inventoryRepository.findByIdAndActiveFalse(inventoryId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> inventoryService.getDeletedInventoryById(inventoryId));
    }

    @Test
    void shouldListAllInventoriesSuccessfully() {
        PagedRequestDto req = PagedRequestDto.builder().page(0).size(10).sortDirection("desc").sortField("createdAt").build();
        Page<Inventory> page = new PageImpl<>(List.of(inventory), PageRequest.of(0,10), 1);
        when(inventoryRepository.findByActiveTrue(any(Pageable.class))).thenReturn(page);

        ApiResponse<PagedResponse<InventoryDto>> response = inventoryService.listAllInventories(req);

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().getTotalElements());
    }
}