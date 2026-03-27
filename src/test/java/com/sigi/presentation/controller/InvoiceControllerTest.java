package com.sigi.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigi.persistence.enums.InvoiceStatus;
import com.sigi.presentation.dto.invoice.InvoiceDto;
import com.sigi.presentation.dto.invoice.NewInvoiceDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.services.service.invoice.InvoiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
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

@ExtendWith(MockitoExtension.class)
class InvoiceControllerTest {

    @Mock
    private InvoiceService invoiceService;

    @InjectMocks
    private InvoiceController invoiceController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID invoiceId;
    private InvoiceDto invoiceDto;
    private NewInvoiceDto newInvoiceDto;

    @Captor
    private ArgumentCaptor<PagedRequestDto> pagedRequestCaptor;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(invoiceController).build();

        invoiceId = UUID.randomUUID();
        invoiceDto = InvoiceDto.builder()
                .id(invoiceId)
                .number("INV-1001")
                .subtotal(BigDecimal.valueOf(1000))
                .tax(BigDecimal.valueOf(190))
                .total(BigDecimal.valueOf(1190))
                .status(InvoiceStatus.ISSUED)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        newInvoiceDto = NewInvoiceDto.builder()
                .orderId(UUID.randomUUID())
                .tax(BigDecimal.valueOf(190))
                .build();
    }

    @Test
    void createInvoice_returnsOkAndBody() throws Exception {
        when(invoiceService.createInvoice(any(NewInvoiceDto.class)))
                .thenReturn(ApiResponse.success("Created", invoiceDto));

        mockMvc.perform(post("/api/v1/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newInvoiceDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.message").isNotEmpty());

        verify(invoiceService, times(1)).createInvoice(any(NewInvoiceDto.class));
    }

    @Test
    void updateInvoice_returnsOkAndUpdatedBody() throws Exception {
        InvoiceDto updated = InvoiceDto.builder()
                .id(invoiceId)
                .number("INV-1001")
                .subtotal(BigDecimal.valueOf(1100))
                .tax(BigDecimal.valueOf(209))
                .total(BigDecimal.valueOf(1309))
                .status(InvoiceStatus.ISSUED)
                .build();

        when(invoiceService.updateInvoice(eq(invoiceId), any(NewInvoiceDto.class)))
                .thenReturn(ApiResponse.success("Updated", updated));

        mockMvc.perform(put("/api/v1/invoices/{id}", invoiceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newInvoiceDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.data.subtotal").value(1100));

        verify(invoiceService, times(1)).updateInvoice(eq(invoiceId), any(NewInvoiceDto.class));
    }

    @Test
    void deleteInvoice_returnsOk() throws Exception {
        when(invoiceService.deleteInvoice(invoiceId)).thenReturn(ApiResponse.success("Deleted", null));

        mockMvc.perform(delete("/api/v1/invoices/{id}", invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());

        verify(invoiceService, times(1)).deleteInvoice(invoiceId);
    }

    @Test
    void restoreInvoice_returnsOk() throws Exception {
        when(invoiceService.restoreInvoice(invoiceId)).thenReturn(ApiResponse.success("Restored", null));

        mockMvc.perform(post("/api/v1/invoices/{id}/restore", invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());

        verify(invoiceService, times(1)).restoreInvoice(invoiceId);
    }

    @Test
    void getInvoiceById_returnsInvoice() throws Exception {
        when(invoiceService.getInvoiceById(invoiceId)).thenReturn(ApiResponse.success("Found", invoiceDto));

        mockMvc.perform(get("/api/v1/invoices/{id}", invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.code").value(200));

        verify(invoiceService, times(1)).getInvoiceById(invoiceId);
    }

    @Test
    void getDeletedInvoiceById_returnsInvoice() throws Exception {
        when(invoiceService.getDeletedInvoiceById(invoiceId)).thenReturn(ApiResponse.success("Found", invoiceDto));

        mockMvc.perform(get("/api/v1/invoices/deleted/{id}", invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(invoiceId.toString()));

        verify(invoiceService, times(1)).getDeletedInvoiceById(invoiceId);
    }

    @Test
    void listAllActiveInvoices_callsService_withPagedRequest() throws Exception {
        Page<InvoiceDto> page = new PageImpl<>(List.of(invoiceDto), PageRequest.of(0, 10), 1);
        when(invoiceService.listAllActiveInvoices(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/invoices")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());

        verify(invoiceService, times(1)).listAllActiveInvoices(pagedRequestCaptor.capture());
        PagedRequestDto captured = pagedRequestCaptor.getValue();
        assertThat(captured.getPage()).isEqualTo(0);
        assertThat(captured.getSize()).isEqualTo(10);
    }

    @Test
    void listAllDeletedInvoices_callsService() throws Exception {
        when(invoiceService.listAllDeletedInvoices(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/invoices/deleted")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(invoiceService, times(1)).listAllDeletedInvoices(any(PagedRequestDto.class));
    }

    @Test
    void getInvoiceByNumber_callsService() throws Exception {
        when(invoiceService.getInvoiceByNumber("INV-1001")).thenReturn(ApiResponse.success("Found", invoiceDto));

        mockMvc.perform(get("/api/v1/invoices/by-number")
                        .param("number", "INV-1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.number").value("INV-1001"));

        verify(invoiceService, times(1)).getInvoiceByNumber("INV-1001");
    }

    @Test
    void getInvoiceByOrderId_callsService() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(invoiceService.getInvoiceByOrder(orderId)).thenReturn(ApiResponse.success("Found", invoiceDto));

        mockMvc.perform(get("/api/v1/invoices/by-order")
                        .param("orderId", orderId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(invoiceId.toString()));

        verify(invoiceService, times(1)).getInvoiceByOrder(orderId);
    }

    @Test
    void listInvoicesByClient_callsService() throws Exception {
        when(invoiceService.listInvoicesByClient(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/invoices/by-client")
                        .param("searchId", UUID.randomUUID().toString())
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(invoiceService, times(1)).listInvoicesByClient(any(PagedRequestDto.class));
    }

    @Test
    void listInvoicesByStatus_callsService() throws Exception {
        when(invoiceService.listInvoicesByStatus(eq(InvoiceStatus.PAID), any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/invoices/by-status")
                        .param("status", "PAID")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(invoiceService, times(1)).listInvoicesByStatus(eq(InvoiceStatus.PAID), any(PagedRequestDto.class));
    }

    @Test
    void listDeletedInvoicesByNumber_callsService() throws Exception {
        when(invoiceService.listDeletedInvoicesByNumber(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/invoices/deleted/by-number")
                        .param("searchValue", "INV")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(invoiceService, times(1)).listDeletedInvoicesByNumber(any(PagedRequestDto.class));
    }

    @Test
    void issueInvoice_callsService_andReturnsDto() throws Exception {
        when(invoiceService.issueInvoice(invoiceId)).thenReturn(ApiResponse.success("Issued", invoiceDto));

        mockMvc.perform(post("/api/v1/invoices/{id}/issue", invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(invoiceId.toString()));

        verify(invoiceService, times(1)).issueInvoice(invoiceId);
    }

    @Test
    void payInvoice_callsService_andReturnsDto() throws Exception {
        InvoiceDto paid = InvoiceDto.builder()
                .id(invoiceId)
                .number("INV-1001")
                .status(InvoiceStatus.PAID)
                .build();

        when(invoiceService.payInvoice(invoiceId)).thenReturn(ApiResponse.success("Paid", paid));

        mockMvc.perform(post("/api/v1/invoices/{id}/pay", invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"));

        verify(invoiceService, times(1)).payInvoice(invoiceId);
    }

    @Test
    void cancelInvoice_callsService_andReturnsOk() throws Exception {
        when(invoiceService.cancelInvoice(invoiceId)).thenReturn(ApiResponse.success("Canceled", null));

        mockMvc.perform(post("/api/v1/invoices/{id}/cancel", invoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());

        verify(invoiceService, times(1)).cancelInvoice(invoiceId);
    }
}