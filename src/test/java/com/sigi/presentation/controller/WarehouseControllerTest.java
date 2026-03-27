package com.sigi.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.warehouse.NewWarehouseDto;
import com.sigi.presentation.dto.warehouse.WarehouseDto;
import com.sigi.services.service.warehouse.WarehouseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for WarehouseController using MockMvc (standalone) and Mockito.
 * Covers routing, request binding, response shape and service interaction.
 */
@ExtendWith(MockitoExtension.class)
class WarehouseControllerTest {

    @Mock
    private WarehouseService warehouseService;

    @InjectMocks
    private WarehouseController warehouseController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID warehouseId;
    private WarehouseDto warehouseDto;
    private NewWarehouseDto newWarehouseDto;

    @Captor
    private ArgumentCaptor<PagedRequestDto> pagedRequestCaptor;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(warehouseController).build();

        warehouseId = UUID.randomUUID();
        warehouseDto = WarehouseDto.builder()
                .id(warehouseId)
                .name("Central Warehouse")
                .location("Zona Industrial")
                .totalCapacity(50000)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        newWarehouseDto = NewWarehouseDto.builder()
                .name("Central Warehouse")
                .location("Zona Industrial")
                .totalCapacity(50000)
                .build();
    }

    @Test
    void createWarehouse_delegatesToService_andReturnsDto() throws Exception {
        when(warehouseService.createWarehouse(any(NewWarehouseDto.class)))
                .thenReturn(ApiResponse.success("Created", warehouseDto));

        mockMvc.perform(post("/api/v1/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newWarehouseDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(warehouseId.toString()))
                .andExpect(jsonPath("$.data.name").value("Central Warehouse"));

        verify(warehouseService, times(1)).createWarehouse(any(NewWarehouseDto.class));
    }

    @Test
    void updateWarehouse_delegatesToService_andReturnsUpdatedDto() throws Exception {
        WarehouseDto updated = WarehouseDto.builder()
                .id(warehouseId)
                .name("Updated Warehouse")
                .location("New Location")
                .totalCapacity(60000)
                .build();

        when(warehouseService.updateWarehouse(eq(warehouseId), any(NewWarehouseDto.class)))
                .thenReturn(ApiResponse.success("Updated", updated));

        mockMvc.perform(put("/api/v1/warehouses/{id}", warehouseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newWarehouseDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(warehouseId.toString()))
                .andExpect(jsonPath("$.data.name").value("Updated Warehouse"))
                .andExpect(jsonPath("$.data.totalCapacity").value(60000));

        verify(warehouseService, times(1)).updateWarehouse(eq(warehouseId), any(NewWarehouseDto.class));
    }

    @Test
    void deleteWarehouse_callsService_andReturnsOk() throws Exception {
        when(warehouseService.deleteWarehouse(warehouseId)).thenReturn(ApiResponse.success("Deleted", null));

        mockMvc.perform(delete("/api/v1/warehouses/{id}", warehouseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Deleted"));

        verify(warehouseService, times(1)).deleteWarehouse(warehouseId);
    }

    @Test
    void restoreWarehouse_callsService_andReturnsOk() throws Exception {
        when(warehouseService.restoreWarehouse(warehouseId)).thenReturn(ApiResponse.success("Restored", null));

        mockMvc.perform(put("/api/v1/warehouses/{id}/restore", warehouseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Restored"));

        verify(warehouseService, times(1)).restoreWarehouse(warehouseId);
    }

    @Test
    void getWarehouseById_returnsWarehouse() throws Exception {
        when(warehouseService.getWarehouseById(warehouseId)).thenReturn(ApiResponse.success("Found", warehouseDto));

        mockMvc.perform(get("/api/v1/warehouses/{id}", warehouseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(warehouseId.toString()))
                .andExpect(jsonPath("$.code").value(200));

        verify(warehouseService, times(1)).getWarehouseById(warehouseId);
    }

    @Test
    void getDeletedWarehouseById_callsService() throws Exception {
        when(warehouseService.getDeletedWarehouseById(warehouseId)).thenReturn(ApiResponse.success("Found", warehouseDto));

        mockMvc.perform(get("/api/v1/warehouses/deleted/{id}", warehouseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(warehouseId.toString()));

        verify(warehouseService, times(1)).getDeletedWarehouseById(warehouseId);
    }

    @Test
    void listAllActiveWarehouse_bindsPagedRequest_andCallsService() throws Exception {
        when(warehouseService.listAllActiveWarehouse(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/warehouses")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortField", "createdAt")
                        .param("sortDirection", "desc"))
                .andExpect(status().isOk());

        verify(warehouseService, times(1)).listAllActiveWarehouse(pagedRequestCaptor.capture());
        PagedRequestDto captured = pagedRequestCaptor.getValue();
        assertThat(captured.getPage()).isEqualTo(0);
        assertThat(captured.getSize()).isEqualTo(10);
        assertThat(captured.getSortField()).isEqualTo("createdAt");
    }

    @Test
    void listAllDeletedWarehouse_callsService() throws Exception {
        when(warehouseService.listAllDeletedWarehouse(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/warehouses/deleted")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(warehouseService, times(1)).listAllDeletedWarehouse(any(PagedRequestDto.class));
    }

    @Test
    void listWarehouseByCapacityGreaterOrEqual_bindsParams_andCallsService() throws Exception {
        when(warehouseService.listWarehouseByCapacityGreaterOrEqual(eq(1000), any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/warehouses/by-capacity")
                        .param("capacity", "1000")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(warehouseService, times(1)).listWarehouseByCapacityGreaterOrEqual(eq(1000), any(PagedRequestDto.class));
    }

    @Test
    void listWarehouseByName_andListDeletedWarehouseByName_callsService_withPagedRequest() throws Exception {
        when(warehouseService.listWarehouseByName(any(PagedRequestDto.class))).thenReturn(ApiResponse.success("OK", null));
        when(warehouseService.listDeletedWarehouseByName(any(PagedRequestDto.class))).thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/warehouses/by-name")
                        .param("searchValue", "Central")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/warehouses/deleted/by-name")
                        .param("searchValue", "Central")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(warehouseService, times(1)).listWarehouseByName(any(PagedRequestDto.class));
        verify(warehouseService, times(1)).listDeletedWarehouseByName(any(PagedRequestDto.class));
    }
}