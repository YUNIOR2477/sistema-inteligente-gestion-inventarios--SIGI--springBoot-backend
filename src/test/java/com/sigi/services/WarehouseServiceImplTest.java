package com.sigi.services;

import com.sigi.persistence.entity.Warehouse;
import com.sigi.persistence.repository.InventoryRepository;
import com.sigi.persistence.repository.WarehouseRepository;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.warehouse.NewWarehouseDto;
import com.sigi.presentation.dto.warehouse.WarehouseDto;
import com.sigi.services.service.warehouse.WarehouseServiceImpl;
import com.sigi.util.DtoMapper;
import com.sigi.util.PersistenceMethod;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.data.domain.*;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class WarehouseServiceImplTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private DtoMapper dtoMapper;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private PersistenceMethod persistenceMethod;

    @InjectMocks
    private WarehouseServiceImpl warehouseService;

    private NewWarehouseDto newDto;
    private Warehouse entity;

    @BeforeEach
    void setUp() {
        meterRegistry = new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        warehouseService = new WarehouseServiceImpl(
                warehouseRepository,
                inventoryRepository,
                dtoMapper,
                meterRegistry,   // instancia real
                persistenceMethod
        );

        newDto = NewWarehouseDto.builder()
                .name("Central")
                .location("Zona Industrial")
                .totalCapacity(1000)
                .build();

        entity = Warehouse.builder()
                .id(UUID.randomUUID())
                .name("Central")
                .location("Zona Industrial")
                .totalCapacity(1000)
                .active(true)
                .createdAt(LocalDateTime.now(ZoneId.of("America/Bogota")))
                .build();
    }

    @Test
    void createWarehouse_success() {
        when(warehouseRepository.existsByName(newDto.getName())).thenReturn(false);
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("tester@example.com");
        when(warehouseRepository.save(any(Warehouse.class))).thenAnswer(inv -> {
            Warehouse w = inv.getArgument(0);
            w.setId(UUID.randomUUID());
            return w;
        });
        when(dtoMapper.toWarehouseDto(any(Warehouse.class))).thenReturn(WarehouseDto.builder().name("Central").build());

        var response = warehouseService.createWarehouse(newDto);

        assertNotNull(response);
        verify(warehouseRepository, times(1)).save(any(Warehouse.class));
    }

    @Test
    void createWarehouse_whenNameExistsAndActive_thenThrow() {
        when(warehouseRepository.existsByName(newDto.getName())).thenReturn(true);
        Warehouse existing = Warehouse.builder().name("Central").active(true).build();
        when(persistenceMethod.getWarehouseByName(newDto.getName())).thenReturn(existing);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> warehouseService.createWarehouse(newDto));
        assertTrue(ex.getMessage().contains("already") || ex.getMessage().length() > 0);
        verify(warehouseRepository, never()).save(any());
    }

    @Test
    void createWarehouse_whenNameExistsAndInactive_thenThrowInactiveMessage() {
        when(warehouseRepository.existsByName(newDto.getName())).thenReturn(true);
        Warehouse existing = Warehouse.builder().name("Central").active(false).build();
        when(persistenceMethod.getWarehouseByName(newDto.getName())).thenReturn(existing);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> warehouseService.createWarehouse(newDto));
        assertTrue(ex.getMessage().length() > 0);
        verify(warehouseRepository, never()).save(any());
    }

    @Test
    void updateWarehouse_success() {
        UUID id = UUID.randomUUID();
        Warehouse existing = Warehouse.builder()
                .id(id)
                .name("Old")
                .active(true)
                .totalCapacity(500)
                .build();

        NewWarehouseDto updateDto = NewWarehouseDto.builder()
                .name("Central Updated")
                .location("New Location")
                .totalCapacity(2000)
                .build();

        when(persistenceMethod.getWarehouseById(id)).thenReturn(existing);
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("tester@example.com");
        when(warehouseRepository.save(existing)).thenReturn(existing);
        when(dtoMapper.toWarehouseDto(existing)).thenReturn(WarehouseDto.builder().name("Central Updated").build());

        var resp = warehouseService.updateWarehouse(id, updateDto);

        assertNotNull(resp);
        verify(warehouseRepository, times(1)).save(existing);
        assertEquals("Central Updated", resp.getData().getName());
    }

    @Test
    void deleteWarehouse_withActiveInventory_throws() {
        UUID id = UUID.randomUUID();
        Warehouse existing = Warehouse.builder().id(id).active(true).build();
        when(persistenceMethod.getWarehouseById(id)).thenReturn(existing);
        when(inventoryRepository.existsByWarehouseIdAndActiveTrue(id)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> warehouseService.deleteWarehouse(id));
        assertEquals("Cannot delete warehouse with active inventory", ex.getMessage());
        verify(warehouseRepository, never()).save(any());
    }

    @Test
    void deleteWarehouse_success() {
        UUID id = UUID.randomUUID();
        Warehouse existing = Warehouse.builder().id(id).active(true).build();
        when(persistenceMethod.getWarehouseById(id)).thenReturn(existing);
        when(inventoryRepository.existsByWarehouseIdAndActiveTrue(id)).thenReturn(false);
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("tester@example.com");

        var resp = warehouseService.deleteWarehouse(id);

        assertNotNull(resp);
        verify(warehouseRepository, times(1)).save(existing);
        assertFalse(existing.getActive());
        assertNotNull(existing.getDeletedAt());
    }

    @Test
    void restoreWarehouse_conflictCapacity_throws() {
        UUID id = UUID.randomUUID();
        Warehouse existing = Warehouse.builder().id(id).active(false).totalCapacity(10).name("W1").build();
        when(persistenceMethod.getWarehouseById(id)).thenReturn(existing);
        when(warehouseRepository.existsByNameAndActiveTrue(existing.getName())).thenReturn(false);
        when(inventoryRepository.sumUsedCapacityByWarehouseId(existing.getId())).thenReturn(Optional.of(BigDecimal.valueOf(20)));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> warehouseService.restoreWarehouse(id));
        assertTrue(ex.getMessage().contains("used capacity"));
        verify(warehouseRepository, never()).save(any());
    }

    @Test
    void restoreWarehouse_success() {
        UUID id = UUID.randomUUID();
        Warehouse existing = Warehouse.builder().id(id).active(false).totalCapacity(100).name("W1").build();
        when(persistenceMethod.getWarehouseById(id)).thenReturn(existing);
        when(warehouseRepository.existsByNameAndActiveTrue(existing.getName())).thenReturn(false);
        when(inventoryRepository.sumUsedCapacityByWarehouseId(existing.getId())).thenReturn(Optional.of(BigDecimal.valueOf(10)));
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("tester@example.com");

        var resp = warehouseService.restoreWarehouse(id);

        assertNotNull(resp);
        verify(warehouseRepository, times(1)).save(existing);
        assertTrue(existing.getActive());
        assertNull(existing.getDeletedAt());
    }

    @Test
    void validateAvailableCapacity_trueAndFalse() {
        UUID id = UUID.randomUUID();
        Warehouse w = Warehouse.builder().id(id).totalCapacity(100).build();
        when(warehouseRepository.findById(id)).thenReturn(Optional.of(w));
        when(inventoryRepository.sumUsedCapacityByWarehouseId(id)).thenReturn(Optional.of(BigDecimal.valueOf(20)));

        var okResp = warehouseService.validateAvailableCapacity(id, BigDecimal.valueOf(50));
        assertTrue(okResp.getData());

        var notOkResp = warehouseService.validateAvailableCapacity(id, BigDecimal.valueOf(90));
        assertFalse(notOkResp.getData());
    }

    @Test
    void listAllActiveWarehouse_returnsPaged() {
        PagedRequestDto paged = new PagedRequestDto();
        paged.setPage(0);
        paged.setSize(10);
        paged.setSortDirection("ASC");
        paged.setSortField("name");

        Warehouse w1 = Warehouse.builder().id(UUID.randomUUID()).name("A").active(true).build();
        Page<Warehouse> page = new PageImpl<>(List.of(w1), PageRequest.of(0, 10), 1);
        when(warehouseRepository.findByActiveTrue(any(Pageable.class))).thenReturn(page);
        when(dtoMapper.toWarehouseDtoPage(page)).thenReturn(new PageImpl<>(List.of(WarehouseDto.builder().name("A").build())));

        var resp = warehouseService.listAllActiveWarehouse(paged);

        assertNotNull(resp);
        verify(warehouseRepository, times(1)).findByActiveTrue(any(Pageable.class));
    }

    @Test
    void getDeletedWarehouseById_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(warehouseRepository.findByIdAndActiveFalse(id)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> warehouseService.getDeletedWarehouseById(id));
    }
}