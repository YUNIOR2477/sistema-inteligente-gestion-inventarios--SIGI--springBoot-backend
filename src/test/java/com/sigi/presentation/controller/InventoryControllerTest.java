package com.sigi.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sigi.presentation.dto.inventory.*;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.services.service.inventory.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@ExtendWith(MockitoExtension.class)

class InventoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private InventoryController inventoryController;

    private ObjectMapper objectMapper = new ObjectMapper();

    private UUID sampleInventoryId;
    private NewInventoryDto newInventoryDto;
    private InventoryDto inventoryDto;

    @Captor
    private ArgumentCaptor<PagedRequestDto> pagedRequestCaptor;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders.standaloneSetup(inventoryController).build();

        sampleInventoryId = UUID.randomUUID();

        newInventoryDto = NewInventoryDto.builder()
                .productId(UUID.randomUUID())
                .warehouseId(UUID.randomUUID())
                .location("Shelf A3")
                .lot("LOT-2026-001")
                .productionDate(LocalDate.now().minusDays(10))
                .expirationDate(LocalDate.now().plusMonths(6))
                .availableQuantity(BigDecimal.valueOf(150))
                .reservedQuantity(BigDecimal.valueOf(10))
                .build();

        inventoryDto = InventoryDto.builder()
                .id(sampleInventoryId)
                .product(null)
                .warehouse(null)
                .location(newInventoryDto.getLocation())
                .lot(newInventoryDto.getLot())
                .productionDate(newInventoryDto.getProductionDate())
                .expirationDate(newInventoryDto.getExpirationDate())
                .availableQuantity(newInventoryDto.getAvailableQuantity())
                .reservedQuantity(newInventoryDto.getReservedQuantity())
                .active(true)
                .build();
    }

    @Test
    void createInventory_returnsOkAndBody() throws Exception {
        when(inventoryService.createInventory(any(NewInventoryDto.class)))
                .thenReturn(ApiResponse.success("Created", inventoryDto));

        mockMvc.perform(post("/api/v1/inventories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newInventoryDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(sampleInventoryId.toString()))
                .andExpect(jsonPath("$.message").isNotEmpty());

        verify(inventoryService, times(1)).createInventory(any(NewInventoryDto.class));
    }

    @Test
    void updateInventory_returnsOkAndUpdatedBody() throws Exception {
        InventoryDto updated = InventoryDto.builder()
                .id(sampleInventoryId)
                .location("Shelf B2")
                .lot(newInventoryDto.getLot())
                .availableQuantity(BigDecimal.valueOf(200))
                .reservedQuantity(BigDecimal.valueOf(5))
                .build();

        when(inventoryService.updateInventory(eq(sampleInventoryId), any(NewInventoryDto.class)))
                .thenReturn(ApiResponse.success("Updated", updated));

        mockMvc.perform(put("/api/v1/inventories/{id}", sampleInventoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newInventoryDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(sampleInventoryId.toString()))
                .andExpect(jsonPath("$.data.location").value("Shelf B2"));

        verify(inventoryService, times(1)).updateInventory(eq(sampleInventoryId), any(NewInventoryDto.class));
    }

    @Test
    void deleteInventory_returnsOk() throws Exception {
        when(inventoryService.deleteInventory(sampleInventoryId)).thenReturn(ApiResponse.success("Deleted", null));

        mockMvc.perform(delete("/api/v1/inventories/{id}", sampleInventoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());

        verify(inventoryService, times(1)).deleteInventory(sampleInventoryId);
    }

    @Test
    void restoreInventory_returnsOk() throws Exception {
        when(inventoryService.restoreInventory(sampleInventoryId)).thenReturn(ApiResponse.success("Restored", null));

        mockMvc.perform(put("/api/v1/inventories/{id}/restore", sampleInventoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());

        verify(inventoryService, times(1)).restoreInventory(sampleInventoryId);
    }

    @Test
    void getInventoryById_returnsInventory() throws Exception {
        when(inventoryService.getInventoryById(sampleInventoryId)).thenReturn(ApiResponse.success("Found", inventoryDto));

        mockMvc.perform(get("/api/v1/inventories/{id}", sampleInventoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(sampleInventoryId.toString()))
                .andExpect(jsonPath("$.code").value(200));

        verify(inventoryService, times(1)).getInventoryById(sampleInventoryId);
    }

    @Test
    void getDeletedInventoryById_returnsInventory() throws Exception {
        when(inventoryService.getDeletedInventoryById(sampleInventoryId)).thenReturn(ApiResponse.success("Found", inventoryDto));

        mockMvc.perform(get("/api/v1/inventories/deleted/{id}", sampleInventoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(sampleInventoryId.toString()));

        verify(inventoryService, times(1)).getDeletedInventoryById(sampleInventoryId);
    }

    @Test
    void listAllInventories_returnsPagedResponse() throws Exception {
        PagedRequestDto request = new PagedRequestDto();
        request.setPage(0);
        request.setSize(10);

        Page<InventoryDto> page = new PageImpl<>(List.of(inventoryDto), PageRequest.of(0, 10), 1);
        when(inventoryService.listAllInventories(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null)); // controller returns ApiResponse<PagedResponse<InventoryDto>>

        // We only assert controller wiring and that service is called with parsed model attribute
        mockMvc.perform(get("/api/v1/inventories")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());

        verify(inventoryService, times(1)).listAllInventories(pagedRequestCaptor.capture());
        PagedRequestDto captured = pagedRequestCaptor.getValue();
        assertThat(captured.getPage()).isEqualTo(0);
        assertThat(captured.getSize()).isEqualTo(10);
    }

    @Test
    void listDeletedInventories_returnsOk() throws Exception {
        when(inventoryService.listAllDeletedInventories(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/inventories/deleted")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(inventoryService, times(1)).listAllDeletedInventories(any(PagedRequestDto.class));
    }

    @Test
    void listInventoriesByWarehouse_callsService() throws Exception {
        when(inventoryService.listInventoriesByWarehouse(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/inventories/by-warehouse")
                        .param("searchId", UUID.randomUUID().toString())
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(inventoryService, times(1)).listInventoriesByWarehouse(any(PagedRequestDto.class));
    }

    @Test
    void listInventoriesByProduct_callsService() throws Exception {
        when(inventoryService.listInventoriesByProduct(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/inventories/by-product")
                        .param("searchId", UUID.randomUUID().toString())
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(inventoryService, times(1)).listInventoriesByProduct(any(PagedRequestDto.class));
    }

    @Test
    void listInventoriesByLowStock_callsService() throws Exception {
        when(inventoryService.listInventoriesByLowStock(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/inventories/low-stock")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(inventoryService, times(1)).listInventoriesByLowStock(any(PagedRequestDto.class));
    }

    @Test
    void listAvailableInventoriesByWarehouse_callsService() throws Exception {
        when(inventoryService.listAvailableInventoriesByWarehouse(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/inventories/by-available-warehouse")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(inventoryService, times(1)).listAvailableInventoriesByWarehouse(any(PagedRequestDto.class));
    }

    @Test
    void listInventoriesByProductName_callsService() throws Exception {
        when(inventoryService.listInventoriesByProductName(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/inventories/by-product-name")
                        .param("searchValue", "widget")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(inventoryService, times(1)).listInventoriesByProductName(any(PagedRequestDto.class));
    }

    @Test
    void listInventoriesByProductSku_callsService() throws Exception {
        when(inventoryService.listInventoriesByProductSku(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/inventories/by-product-sku")
                        .param("searchValue", "SKU-1")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(inventoryService, times(1)).listInventoriesByProductSku(any(PagedRequestDto.class));
    }

    @Test
    void listDeletedInventoriesByProductName_callsService() throws Exception {
        when(inventoryService.listDeletedInventoriesByProductName(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/inventories/deleted/by-product-name")
                        .param("searchValue", "widget")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(inventoryService, times(1)).listDeletedInventoriesByProductName(any(PagedRequestDto.class));
    }

    @Test
    void registerInventoryEntry_callsService_withBody() throws Exception {
        NewEntryDto entry = NewEntryDto.builder()
                .inventoryId(UUID.randomUUID())
                .quantity(BigDecimal.valueOf(10))
                .build();

        when(inventoryService.registerInventoryEntry(any(NewEntryDto.class)))
                .thenReturn(ApiResponse.success("Registered", null));

        mockMvc.perform(post("/api/v1/inventories/register-entry")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entry)))
                .andExpect(status().isOk());

        verify(inventoryService, times(1)).registerInventoryEntry(any(NewEntryDto.class));
    }

    @Test
    void registerInventoryTransfer_callsService_withBody() throws Exception {
        ExitTransferDto transfer = ExitTransferDto.builder()
                .originInventoryId(UUID.randomUUID())
                .destinationInventoryId(UUID.randomUUID())
                .quantity(BigDecimal.valueOf(5))
                .motive("Transfer")
                .build();

        when(inventoryService.registerInventoryTransfer(any(ExitTransferDto.class)))
                .thenReturn(ApiResponse.success("Transfer registered", null));

        mockMvc.perform(post("/api/v1/inventories/register-transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transfer)))
                .andExpect(status().isOk());

        verify(inventoryService, times(1)).registerInventoryTransfer(any(ExitTransferDto.class));
    }

    @Test
    void registerInventoryExitForDisposal_callsService_withBody() throws Exception {
        ExitDisposalDto disposal = ExitDisposalDto.builder()
                .inventoryId(UUID.randomUUID())
                .quantity(BigDecimal.valueOf(2))
                .motive("Expired")
                .build();

        when(inventoryService.registerInventoryExitForDisposal(any(ExitDisposalDto.class)))
                .thenReturn(ApiResponse.success("Disposal registered", null));

        mockMvc.perform(post("/api/v1/inventories/register-disposal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(disposal)))
                .andExpect(status().isOk());

        verify(inventoryService, times(1)).registerInventoryExitForDisposal(any(ExitDisposalDto.class));
    }
}