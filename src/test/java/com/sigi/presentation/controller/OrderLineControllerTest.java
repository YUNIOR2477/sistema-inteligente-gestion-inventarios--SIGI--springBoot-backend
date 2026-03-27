package com.sigi.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigi.presentation.dto.order.NewOrderLineDto;
import com.sigi.presentation.dto.order.OrderLineDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.services.service.order.line.OrderLineService;
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
 * Unit tests for OrderLineController using MockMvc (standalone) and Mockito.
 * Focus: routing, request binding, response shape and service interaction.
 */
@ExtendWith(MockitoExtension.class)
class OrderLineControllerTest {

    @Mock
    private OrderLineService orderLineService;

    @InjectMocks
    private OrderLineController orderLineController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID lineId;
    private OrderLineDto orderLineDto;
    private NewOrderLineDto newOrderLineDto;

    @Captor
    private ArgumentCaptor<PagedRequestDto> pagedRequestCaptor;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderLineController).build();

        lineId = UUID.randomUUID();
        orderLineDto = OrderLineDto.builder()
                .id(lineId)
                .orderId(UUID.randomUUID())
                .quantity(BigDecimal.valueOf(10))
                .unitPrice(BigDecimal.valueOf(25000.50))
                .lot("LOT-2026-001")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        newOrderLineDto = NewOrderLineDto.builder()
                .quantity(BigDecimal.valueOf(5))
                .inventoryId(UUID.randomUUID())
                .build();
    }

    @Test
    void createOrderLine_delegatesToService_andReturnsDto() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderLineService.createOrderLine(eq(orderId), any(NewOrderLineDto.class)))
                .thenReturn(ApiResponse.success("Created", orderLineDto));

        mockMvc.perform(post("/api/v1/order-lines")
                        .param("orderId", orderId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newOrderLineDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(lineId.toString()))
                .andExpect(jsonPath("$.data.lot").value("LOT-2026-001"));

        verify(orderLineService, times(1)).createOrderLine(eq(orderId), any(NewOrderLineDto.class));
    }

    @Test
    void updateOrderLine_delegatesToService_andReturnsUpdatedDto() throws Exception {
        OrderLineDto updated = OrderLineDto.builder()
                .id(lineId)
                .quantity(BigDecimal.valueOf(8))
                .unitPrice(BigDecimal.valueOf(26000))
                .build();

        when(orderLineService.updateOrderLine(eq(lineId), any(NewOrderLineDto.class)))
                .thenReturn(ApiResponse.success("Updated", updated));

        mockMvc.perform(put("/api/v1/order-lines/{id}", lineId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newOrderLineDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(lineId.toString()))
                .andExpect(jsonPath("$.data.quantity").value(8));

        verify(orderLineService, times(1)).updateOrderLine(eq(lineId), any(NewOrderLineDto.class));
    }

    @Test
    void deleteOrderLine_callsService_andReturnsOk() throws Exception {
        when(orderLineService.deleteOrderLine(lineId)).thenReturn(ApiResponse.success("Deleted", null));

        mockMvc.perform(delete("/api/v1/order-lines/{id}", lineId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Deleted"));

        verify(orderLineService, times(1)).deleteOrderLine(lineId);
    }

    @Test
    void restoreOrderLine_callsService_andReturnsOk() throws Exception {
        when(orderLineService.restoreOrderLine(lineId)).thenReturn(ApiResponse.success("Restored", null));

        mockMvc.perform(post("/api/v1/order-lines/{id}/restore", lineId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Restored"));

        verify(orderLineService, times(1)).restoreOrderLine(lineId);
    }

    @Test
    void getOrderLineById_returnsOrderLine() throws Exception {
        when(orderLineService.getOrderLineById(lineId)).thenReturn(ApiResponse.success("Found", orderLineDto));

        mockMvc.perform(get("/api/v1/order-lines/{id}", lineId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(lineId.toString()))
                .andExpect(jsonPath("$.code").value(200));

        verify(orderLineService, times(1)).getOrderLineById(lineId);
    }

    @Test
    void getDeletedOrderLineById_callsService() throws Exception {
        when(orderLineService.getDeletedOrderLineById(lineId)).thenReturn(ApiResponse.success("Found", orderLineDto));

        mockMvc.perform(get("/api/v1/order-lines/deleted/{id}", lineId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(lineId.toString()));

        verify(orderLineService, times(1)).getDeletedOrderLineById(lineId);
    }

    @Test
    void listLinesByOrder_passesPagedRequest_andCallsService() throws Exception {
        when(orderLineService.listLinesByOrder(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/order-lines/by-order")
                        .param("searchId", UUID.randomUUID().toString())
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(orderLineService, times(1)).listLinesByOrder(pagedRequestCaptor.capture());
        PagedRequestDto captured = pagedRequestCaptor.getValue();
        assertThat(captured.getPage()).isEqualTo(0);
        assertThat(captured.getSize()).isEqualTo(5);
    }

    @Test
    void listAllActiveLines_passesPagedRequest_andCallsService() throws Exception {
        when(orderLineService.listAllActiveLines(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/order-lines/active")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk());

        verify(orderLineService, times(1)).listAllActiveLines(pagedRequestCaptor.capture());
        PagedRequestDto captured = pagedRequestCaptor.getValue();
        assertThat(captured.getPage()).isEqualTo(1);
        assertThat(captured.getSize()).isEqualTo(10);
    }

    @Test
    void listAllDeletedLines_callsService() throws Exception {
        when(orderLineService.listAllDeletedLines(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/order-lines/deleted")
                        .param("page", "0")
                        .param("size", "3"))
                .andExpect(status().isOk());

        verify(orderLineService, times(1)).listAllDeletedLines(any(PagedRequestDto.class));
    }

    @Test
    void listActiveLinesByProductName_callsService() throws Exception {
        when(orderLineService.listActiveLinesByProductName(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/order-lines/active/by-product-name")
                        .param("searchValue", "Widget")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(orderLineService, times(1)).listActiveLinesByProductName(any(PagedRequestDto.class));
    }

    @Test
    void listDeletedLinesByProductName_callsService() throws Exception {
        when(orderLineService.listDeletedLinesByProductName(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/order-lines/deleted/by-product-name")
                        .param("searchValue", "Widget")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(orderLineService, times(1)).listDeletedLinesByProductName(any(PagedRequestDto.class));
    }
}