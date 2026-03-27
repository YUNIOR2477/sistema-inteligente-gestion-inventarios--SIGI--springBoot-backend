package com.sigi.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigi.presentation.dto.movement.MovementDto;
import com.sigi.presentation.dto.movement.NewMovementDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.services.service.movement.MovementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for MovementController using MockMvc and Mockito.
 * Focus: controller routing, request binding, response shape and service interaction.
 */
@ExtendWith(MockitoExtension.class)
class MovementControllerTest {

    @Mock
    private MovementService movementService;

    @InjectMocks
    private MovementController movementController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID movementId;
    private MovementDto movementDto;
    private NewMovementDto newMovementDto;

    @Captor
    private ArgumentCaptor<PagedRequestDto> pagedRequestCaptor;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(movementController).build();

        movementId = UUID.randomUUID();
        movementDto = MovementDto.builder()
                .id(movementId)
                .type("ENTRY")
                .quantity(BigDecimal.valueOf(10))
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        newMovementDto = NewMovementDto.builder()
                .type(com.sigi.persistence.enums.MovementType.ENTRY)
                .inventoryId(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .quantity(BigDecimal.valueOf(10))
                .motive("Initial stock")
                .build();
    }

    @Test
    void updateMovement_callsService_andReturnsUpdated() throws Exception {
        MovementDto updated = MovementDto.builder()
                .id(movementId)
                .type("ENTRY")
                .quantity(BigDecimal.valueOf(15))
                .build();

        when(movementService.updateMovement(eq(movementId), any(NewMovementDto.class)))
                .thenReturn(ApiResponse.success("Updated", updated));

        mockMvc.perform(put("/api/v1/movements/{id}", movementId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newMovementDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(movementId.toString()))
                .andExpect(jsonPath("$.data.quantity").value(15));

        verify(movementService, times(1)).updateMovement(eq(movementId), any(NewMovementDto.class));
    }

    @Test
    void deleteMovement_callsService_andReturnsOk() throws Exception {
        when(movementService.deleteMovement(movementId)).thenReturn(ApiResponse.success("Deleted", null));

        mockMvc.perform(delete("/api/v1/movements/{id}", movementId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Deleted"));

        verify(movementService, times(1)).deleteMovement(movementId);
    }

    @Test
    void restoreMovement_callsService_andReturnsOk() throws Exception {
        when(movementService.restoreMovement(movementId)).thenReturn(ApiResponse.success("Restored", null));

        mockMvc.perform(put("/api/v1/movements/{id}/restore", movementId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Restored"));

        verify(movementService, times(1)).restoreMovement(movementId);
    }

    @Test
    void getMovementById_returnsMovement() throws Exception {
        when(movementService.getMovementById(movementId)).thenReturn(ApiResponse.success("Found", movementDto));

        mockMvc.perform(get("/api/v1/movements/{id}", movementId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(movementId.toString()))
                .andExpect(jsonPath("$.code").value(200));

        verify(movementService, times(1)).getMovementById(movementId);
    }

    @Test
    void getDeletedMovementById_callsService() throws Exception {
        when(movementService.getDeletedMovementById(movementId)).thenReturn(ApiResponse.success("Found", movementDto));

        mockMvc.perform(get("/api/v1/movements/deleted/{id}", movementId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(movementId.toString()));

        verify(movementService, times(1)).getDeletedMovementById(movementId);
    }

    @Test
    void listAllActiveMovements_passesPagedRequest_toService() throws Exception {
        when(movementService.listAllActiveMovements(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/movements")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortField", "createdAt")
                        .param("sortDirection", "desc"))
                .andExpect(status().isOk());

        verify(movementService, times(1)).listAllActiveMovements(pagedRequestCaptor.capture());
        PagedRequestDto captured = pagedRequestCaptor.getValue();
        assertThat(captured.getPage()).isEqualTo(0);
        assertThat(captured.getSize()).isEqualTo(10);
        assertThat(captured.getSortField()).isEqualTo("createdAt");
    }

    @Test
    void listDeletedMovements_callsService() throws Exception {
        when(movementService.listAllDeletedMovements(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/movements/deleted")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(movementService, times(1)).listAllDeletedMovements(any(PagedRequestDto.class));
    }

    @Test
    void listMovementsByProduct_callsService() throws Exception {
        when(movementService.listMovementsByProduct(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/movements/by-product")
                        .param("searchId", UUID.randomUUID().toString())
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(movementService, times(1)).listMovementsByProduct(any(PagedRequestDto.class));
    }

    @Test
    void listMovementsByOrder_callsService() throws Exception {
        when(movementService.listMovementsByOrder(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/movements/by-order")
                        .param("searchId", UUID.randomUUID().toString())
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(movementService, times(1)).listMovementsByOrder(any(PagedRequestDto.class));
    }

    @Test
    void listMovementsByDispatcher_callsService() throws Exception {
        when(movementService.listMovementsByDispatcher(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/movements/by-dispatcher")
                        .param("searchId", UUID.randomUUID().toString())
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(movementService, times(1)).listMovementsByDispatcher(any(PagedRequestDto.class));
    }

    @Test
    void listMovementsByType_callsService() throws Exception {
        when(movementService.listMovementsByType(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/movements/by-type")
                        .param("searchValue", "ENTRY")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(movementService, times(1)).listMovementsByType(any(PagedRequestDto.class));
    }

    @Test
    void listMovementsDeletedByType_callsService() throws Exception {
        when(movementService.listDeletedMovementsByType(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/movements/deleted/by-type")
                        .param("searchValue", "EXIT")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(movementService, times(1)).listDeletedMovementsByType(any(PagedRequestDto.class));
    }
}