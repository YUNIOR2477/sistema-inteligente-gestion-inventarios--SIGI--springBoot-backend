package com.sigi.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigi.presentation.dto.order.NewOrderDto;
import com.sigi.presentation.dto.order.OrderDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.services.service.order.OrderService;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for OrderController using MockMvc (standalone) and Mockito.
 * Focus: routing, request binding, response shape and service interaction.
 */
@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID orderId;
    private OrderDto orderDto;
    private NewOrderDto newOrderDto;

    @Captor
    private ArgumentCaptor<PagedRequestDto> pagedRequestCaptor;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderController).build();

        orderId = UUID.randomUUID();
        orderDto = OrderDto.builder()
                .id(orderId)
                .status("CONFIRMED")
                .total(BigDecimal.valueOf(250000))
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        newOrderDto = NewOrderDto.builder()
                .clientId(UUID.randomUUID())
                .warehouseId(UUID.randomUUID())
                .dispatcherId(UUID.randomUUID())
                .build();
    }

    @Test
    void createOrder_delegatesToService_andReturnsCreatedDto() throws Exception {
        when(orderService.createOrder(any(NewOrderDto.class)))
                .thenReturn(ApiResponse.success("Created", orderDto));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newOrderDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(orderId.toString()))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        verify(orderService, times(1)).createOrder(any(NewOrderDto.class));
    }

    @Test
    void updateOrder_delegatesToService_andReturnsUpdatedDto() throws Exception {
        OrderDto updated = OrderDto.builder()
                .id(orderId)
                .status("DELIVERED")
                .total(BigDecimal.valueOf(260000))
                .build();

        when(orderService.updateOrder(eq(orderId), any(NewOrderDto.class)))
                .thenReturn(ApiResponse.success("Updated", updated));

        mockMvc.perform(put("/api/v1/orders/{id}", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newOrderDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(orderId.toString()))
                .andExpect(jsonPath("$.data.status").value("DELIVERED"));

        verify(orderService, times(1)).updateOrder(eq(orderId), any(NewOrderDto.class));
    }

    @Test
    void deleteOrder_callsService_andReturnsOk() throws Exception {
        when(orderService.deleteOrder(orderId)).thenReturn(ApiResponse.success("Deleted", null));

        mockMvc.perform(delete("/api/v1/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Deleted"));

        verify(orderService, times(1)).deleteOrder(orderId);
    }

    @Test
    void restoreOrder_callsService_andReturnsOk() throws Exception {
        when(orderService.restoreOrder(orderId)).thenReturn(ApiResponse.success("Restored", null));

        mockMvc.perform(put("/api/v1/orders/{id}/restore", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Restored"));

        verify(orderService, times(1)).restoreOrder(orderId);
    }

    @Test
    void getOrderById_returnsOrder() throws Exception {
        when(orderService.getOrderById(orderId)).thenReturn(ApiResponse.success("Found", orderDto));

        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(orderId.toString()))
                .andExpect(jsonPath("$.code").value(200));

        verify(orderService, times(1)).getOrderById(orderId);
    }

    @Test
    void getDeletedOrderById_callsService() throws Exception {
        when(orderService.getDeletedOrderById(orderId)).thenReturn(ApiResponse.success("Found", orderDto));

        mockMvc.perform(get("/api/v1/orders/deleted/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(orderId.toString()));

        verify(orderService, times(1)).getDeletedOrderById(orderId);
    }

    @Test
    void listAllActiveOrders_bindsPagedRequest_andCallsService() throws Exception {
        when(orderService.listAllActiveOrders(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/orders")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortField", "createdAt")
                        .param("sortDirection", "desc"))
                .andExpect(status().isOk());

        verify(orderService, times(1)).listAllActiveOrders(pagedRequestCaptor.capture());
        PagedRequestDto captured = pagedRequestCaptor.getValue();
        assertThat(captured.getPage()).isEqualTo(0);
        assertThat(captured.getSize()).isEqualTo(10);
        assertThat(captured.getSortField()).isEqualTo("createdAt");
    }

    @Test
    void listDeletedOrders_callsService() throws Exception {
        when(orderService.listAllDeletedOrders(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/orders/deleted")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(orderService, times(1)).listAllDeletedOrders(any(PagedRequestDto.class));
    }

    @Test
    void listOrdersByClient_callsService_withPagedRequest() throws Exception {
        when(orderService.listOrdersByClient(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/orders/by-client")
                        .param("searchId", UUID.randomUUID().toString())
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(orderService, times(1)).listOrdersByClient(any(PagedRequestDto.class));
    }

    @Test
    void listOrdersByClientName_callsService() throws Exception {
        when(orderService.listOrdersByClientName(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/orders/by-client-name")
                        .param("searchValue", "Acme")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(orderService, times(1)).listOrdersByClientName(any(PagedRequestDto.class));
    }

    @Test
    void listOrdersByUser_callsService() throws Exception {
        when(orderService.listOrdersByUser(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/orders/by-user")
                        .param("searchId", UUID.randomUUID().toString())
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(orderService, times(1)).listOrdersByUser(any(PagedRequestDto.class));
    }

    @Test
    void listOrdersByInventory_callsService() throws Exception {
        when(orderService.listOrdersByInventory(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/orders/by-inventory")
                        .param("searchId", UUID.randomUUID().toString())
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(orderService, times(1)).listOrdersByInventory(any(PagedRequestDto.class));
    }

    @Test
    void cancelOrder_callsService_andReturnsOk() throws Exception {
        when(orderService.cancelOrder(orderId)).thenReturn(ApiResponse.success("Canceled", null));

        mockMvc.perform(put("/api/v1/orders/{id}/cancel", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Canceled"));

        verify(orderService, times(1)).cancelOrder(orderId);
    }

    @Test
    void changeOrderStatus_callsService_andReturnsOk() throws Exception {
        when(orderService.changeOrderStatus(orderId, "CONFIRMED")).thenReturn(ApiResponse.success("Status changed", null));

        mockMvc.perform(put("/api/v1/orders/{id}/change-status", orderId)
                        .param("status", "CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Status changed"));

        verify(orderService, times(1)).changeOrderStatus(orderId, "CONFIRMED");
    }
}