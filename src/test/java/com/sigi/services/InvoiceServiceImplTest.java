package com.sigi.services;

import com.sigi.persistence.entity.Client;
import com.sigi.persistence.entity.Invoice;
import com.sigi.persistence.entity.Order;
import com.sigi.persistence.entity.User;
import com.sigi.persistence.enums.InvoiceStatus;
import com.sigi.persistence.enums.OrderStatus;
import com.sigi.persistence.enums.RoleList;
import com.sigi.persistence.repository.InvoiceRepository;
import com.sigi.persistence.repository.OrderRepository;
import com.sigi.presentation.dto.invoice.InvoiceDto;
import com.sigi.presentation.dto.invoice.NewInvoiceDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.services.service.inventory.InventoryService;
import com.sigi.services.service.websocket.notification.NotificationService;
import com.sigi.services.service.invoice.InvoiceServiceImpl;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceImplTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DtoMapper dtoMapper;

    @Mock
    private PersistenceMethod persistenceMethod;

    @Mock
    private NotificationService notificationService;

    @Mock
    private InventoryService inventoryService;

    @InjectMocks
    private InvoiceServiceImpl invoiceService;

    private SimpleMeterRegistry meterRegistry;

    private UUID orderId;
    private UUID invoiceId;
    private Order order;
    private Client client;
    private User orderUser;
    private Invoice invoice;
    private InvoiceDto invoiceDto;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        invoiceService = new InvoiceServiceImpl(invoiceRepository, orderRepository, dtoMapper, meterRegistry, persistenceMethod, notificationService, inventoryService);

        orderId = UUID.randomUUID();
        invoiceId = UUID.randomUUID();

        client = Client.builder().id(UUID.randomUUID()).name("ACME").build();
        orderUser = User.builder().id(UUID.randomUUID()).build();

        order = Order.builder()
                .id(orderId)
                .client(client)
                .user(orderUser)
                .status(OrderStatus.DRAFT)
                .total(BigDecimal.valueOf(100))
                .build();

        invoice = Invoice.builder()
                .id(invoiceId)
                .number("INV-123")
                .order(order)
                .client(client)
                .subtotal(BigDecimal.valueOf(100))
                .tax(BigDecimal.valueOf(19))
                .total(BigDecimal.valueOf(119))
                .status(InvoiceStatus.ISSUED)
                .active(true)
                .build();

        invoiceDto = InvoiceDto.builder()
                .id(invoiceId)
                .number(invoice.getNumber())
                .subtotal(invoice.getSubtotal())
                .tax(invoice.getTax())
                .total(invoice.getTotal())
                .status(invoice.getStatus())
                .active(invoice.getActive())
                .build();

        lenient().when(dtoMapper.toInvoiceDto(any(Invoice.class))).thenReturn(invoiceDto);
        lenient().when(dtoMapper.toInvoiceDtoPage(any(Page.class))).thenAnswer(inv -> {
            Page<Invoice> page = inv.getArgument(0);
            return page.map(i -> invoiceDto);
        });
    }

    // ------------------- createInvoice -------------------
    @Test
    void shouldCreateInvoiceSuccessfullyAndConfirmOrder() {
        NewInvoiceDto dto = NewInvoiceDto.builder().orderId(orderId).tax(BigDecimal.valueOf(19)).build();

        when(persistenceMethod.getOrderById(orderId)).thenReturn(order);
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("creator@example.com");
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            i.setId(invoiceId);
            return i;
        });
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        ApiResponse<InvoiceDto> response = invoiceService.createInvoice(dto);

        assertEquals(200, response.getCode());
        assertEquals("Invoice created", response.getMessage());
        // order status must be updated to CONFIRMED and saved
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        verify(orderRepository, times(1)).save(order);
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
        assertTrue(meterRegistry.get("invoice.service.operations").tags("type", "createInvoice").counter().count() >= 1.0);
    }

    @Test
    void shouldThrowWhenCreatingInvoiceIfOrderNotDraft() {
        order.setStatus(OrderStatus.CONFIRMED);
        NewInvoiceDto dto = NewInvoiceDto.builder().orderId(orderId).tax(BigDecimal.valueOf(19)).build();
        when(persistenceMethod.getOrderById(orderId)).thenReturn(order);

        assertThrows(IllegalStateException.class, () -> invoiceService.createInvoice(dto));
        verify(invoiceRepository, never()).save(any());
    }

    // ------------------- updateInvoice -------------------
    @Test
    void shouldUpdateInvoiceSuccessfullyWhenIssued() {
        UUID id = invoiceId;
        NewInvoiceDto dto = NewInvoiceDto.builder().orderId(orderId).tax(BigDecimal.valueOf(10)).build();

        Invoice existing = Invoice.builder().id(id).status(InvoiceStatus.ISSUED).order(order).build();
        when(persistenceMethod.getInvoiceById(id)).thenReturn(existing);
        when(persistenceMethod.getOrderById(orderId)).thenReturn(order);
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("updater@example.com");
        when(invoiceRepository.save(existing)).thenReturn(existing);

        ApiResponse<InvoiceDto> response = invoiceService.updateInvoice(id, dto);

        assertEquals(200, response.getCode());
        assertEquals("Invoice updated", response.getMessage());
        verify(invoiceRepository, times(1)).save(existing);
        assertNotNull(existing.getUpdatedAt());
        assertEquals("updater@example.com", existing.getUpdatedBy());
    }

    @Test
    void shouldThrowWhenUpdatingInvoiceIfNotIssued() {
        UUID id = invoiceId;
        Invoice existing = Invoice.builder().id(id).status(InvoiceStatus.DRAFT).build();
        when(persistenceMethod.getInvoiceById(id)).thenReturn(existing);

        NewInvoiceDto dto = NewInvoiceDto.builder().orderId(orderId).tax(BigDecimal.valueOf(10)).build();

        assertThrows(IllegalStateException.class, () -> invoiceService.updateInvoice(id, dto));
        verify(invoiceRepository, never()).save(any());
    }

    // ------------------- deleteInvoice / restoreInvoice -------------------
    @Test
    void shouldDeleteInvoiceWhenNotPaid() {
        Invoice inv = Invoice.builder().id(invoiceId).status(InvoiceStatus.ISSUED).active(true).build();
        when(persistenceMethod.getInvoiceById(invoiceId)).thenReturn(inv);
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("deleter@example.com");
        when(invoiceRepository.save(inv)).thenReturn(inv);

        ApiResponse<Void> response = invoiceService.deleteInvoice(invoiceId);

        assertEquals(200, response.getCode());
        assertEquals("Invoice deleted", response.getMessage());
        assertFalse(inv.getActive());
        assertNotNull(inv.getDeletedAt());
        assertEquals("deleter@example.com", inv.getDeletedBy());
    }

    @Test
    void shouldThrowWhenDeletingPaidInvoice() {
        Invoice paid = Invoice.builder().id(invoiceId).status(InvoiceStatus.PAID).active(true).build();
        when(persistenceMethod.getInvoiceById(invoiceId)).thenReturn(paid);

        assertThrows(IllegalStateException.class, () -> invoiceService.deleteInvoice(invoiceId));
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void shouldRestoreInvoiceWhenNotPaid() {
        Invoice inv = Invoice.builder().id(invoiceId).status(InvoiceStatus.ISSUED).active(false).build();
        when(persistenceMethod.getInvoiceById(invoiceId)).thenReturn(inv);
        when(invoiceRepository.save(inv)).thenReturn(inv);

        ApiResponse<Void> response = invoiceService.restoreInvoice(invoiceId);

        assertEquals(200, response.getCode());
        assertTrue(inv.getActive());
        assertNull(inv.getDeletedAt());
        assertNull(inv.getDeletedBy());
    }

    @Test
    void shouldThrowWhenRestoringPaidInvoice() {
        Invoice paid = Invoice.builder().id(invoiceId).status(InvoiceStatus.PAID).active(false).build();
        when(persistenceMethod.getInvoiceById(invoiceId)).thenReturn(paid);

        assertThrows(IllegalStateException.class, () -> invoiceService.restoreInvoice(invoiceId));
        verify(invoiceRepository, never()).save(any());
    }

    // ------------------- getInvoiceById / getDeletedInvoiceById -------------------
    @Test
    void shouldGetInvoiceByIdSuccessfullyWhenActive() {
        Invoice active = Invoice.builder().id(invoiceId).active(true).build();
        when(persistenceMethod.getInvoiceById(invoiceId)).thenReturn(active);

        ApiResponse<InvoiceDto> response = invoiceService.getInvoiceById(invoiceId);

        assertEquals(200, response.getCode());
        assertEquals(invoiceDto, response.getData());
    }

    @Test
    void shouldThrowWhenGetInvoiceByIdIfInactive() {
        Invoice inactive = Invoice.builder().id(invoiceId).active(false).build();
        when(persistenceMethod.getInvoiceById(invoiceId)).thenReturn(inactive);

        assertThrows(IllegalArgumentException.class, () -> invoiceService.getInvoiceById(invoiceId));
    }

    @Test
    void shouldGetDeletedInvoiceByIdSuccessfully() {
        Invoice deleted = Invoice.builder().id(invoiceId).active(false).build();
        when(invoiceRepository.findByIdAndActiveFalse(invoiceId)).thenReturn(Optional.of(deleted));

        ApiResponse<InvoiceDto> response = invoiceService.getDeletedInvoiceById(invoiceId);

        assertEquals(200, response.getCode());
        assertEquals(invoiceDto, response.getData());
    }

    @Test
    void shouldThrowWhenDeletedInvoiceNotFound() {
        when(invoiceRepository.findByIdAndActiveFalse(invoiceId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> invoiceService.getDeletedInvoiceById(invoiceId));
    }

    // ------------------- listAllActiveInvoices / listAllDeletedInvoices -------------------
    @Test
    void shouldListAllActiveInvoicesSuccessfully() {
        PagedRequestDto req = PagedRequestDto.builder().page(0).size(10).sortDirection("desc").sortField("createdAt").build();
        Page<Invoice> page = new PageImpl<>(List.of(invoice), PageRequest.of(0, 10), 1);
        when(invoiceRepository.findByActiveTrue(any(Pageable.class))).thenReturn(page);

        ApiResponse<PagedResponse<InvoiceDto>> response = invoiceService.listAllActiveInvoices(req);

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().getTotalElements());
    }

    @Test
    void shouldListAllDeletedInvoicesSuccessfully() {
        PagedRequestDto req = PagedRequestDto.builder().page(0).size(10).sortDirection("desc").sortField("createdAt").build();
        Page<Invoice> page = new PageImpl<>(List.of(invoice));
        when(invoiceRepository.findByActiveFalse(any(Pageable.class))).thenReturn(page);

        ApiResponse<PagedResponse<InvoiceDto>> response = invoiceService.listAllDeletedInvoices(req);

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().getTotalElements());
    }

    // ------------------- getInvoiceByNumber / getInvoiceByOrder -------------------
    @Test
    void shouldGetInvoiceByNumberSuccessfullyWhenActive() {
        when(persistenceMethod.getInvoiceByNumber("INV-123")).thenReturn(invoice);

        ApiResponse<InvoiceDto> response = invoiceService.getInvoiceByNumber("INV-123");

        assertEquals(200, response.getCode());
        assertEquals(invoiceDto, response.getData());
    }

    @Test
    void shouldThrowWhenGetInvoiceByNumberIfInactive() {
        Invoice inactive = Invoice.builder().id(invoiceId).active(false).build();
        when(persistenceMethod.getInvoiceByNumber("INV-123")).thenReturn(inactive);

        assertThrows(IllegalArgumentException.class, () -> invoiceService.getInvoiceByNumber("INV-123"));
    }

    @Test
    void shouldGetInvoiceByOrderSuccessfully() {
        when(invoiceRepository.findByOrderId(orderId)).thenReturn(Optional.of(invoice));

        ApiResponse<InvoiceDto> response = invoiceService.getInvoiceByOrder(orderId);

        assertEquals(200, response.getCode());
        assertEquals(invoiceDto, response.getData());
    }

    @Test
    void shouldThrowWhenGetInvoiceByOrderNotFound() {
        when(invoiceRepository.findByOrderId(orderId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> invoiceService.getInvoiceByOrder(orderId));
    }

    // ------------------- listInvoicesByClient / listInvoicesByStatus -------------------
    @Test
    void shouldListInvoicesByClientSuccessfully() {
        PagedRequestDto req = PagedRequestDto.builder().page(0).size(10).searchValue("ACME").sortDirection("desc").sortField("createdAt").build();
        Page<Invoice> page = new PageImpl<>(List.of(invoice));
        when(invoiceRepository.findByClientNameContainingIgnoreCaseAndActiveTrue(eq("ACME"), any(Pageable.class))).thenReturn(page);

        ApiResponse<PagedResponse<InvoiceDto>> response = invoiceService.listInvoicesByClient(req);

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().getTotalElements());
    }

    @Test
    void shouldListInvoicesByStatusSuccessfully() {
        PagedRequestDto req = PagedRequestDto.builder().page(0).size(10).sortDirection("desc").sortField("createdAt").build();
        Page<Invoice> page = new PageImpl<>(List.of(invoice));
        when(invoiceRepository.findByStatusAndActiveTrue(eq(InvoiceStatus.ISSUED), any(Pageable.class))).thenReturn(page);

        ApiResponse<PagedResponse<InvoiceDto>> response = invoiceService.listInvoicesByStatus(InvoiceStatus.ISSUED, req);

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().getTotalElements());
    }

    // ------------------- issueInvoice / payInvoice / cancelInvoice -------------------
    @Test
    void shouldIssueInvoiceWhenDraft() {
        Invoice draft = Invoice.builder().id(invoiceId).status(InvoiceStatus.DRAFT).order(order).build();
        when(persistenceMethod.getInvoiceById(invoiceId)).thenReturn(draft);
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("issuer@example.com");
        when(invoiceRepository.save(draft)).thenReturn(draft);

        ApiResponse<InvoiceDto> response = invoiceService.issueInvoice(invoiceId);

        assertEquals(200, response.getCode());
        assertEquals("Invoice issued", response.getMessage());
        assertEquals(InvoiceStatus.ISSUED, draft.getStatus());
        verify(notificationService, times(1)).createNotification(eq("Invoice Issued"), contains("Invoice"), eq(order.getUser().getId()));
    }

    @Test
    void shouldThrowWhenIssuingNonDraftInvoice() {
        Invoice notDraft = Invoice.builder().id(invoiceId).status(InvoiceStatus.ISSUED).order(order).build();
        when(persistenceMethod.getInvoiceById(invoiceId)).thenReturn(notDraft);

        assertThrows(IllegalStateException.class, () -> invoiceService.issueInvoice(invoiceId));
    }

    @Test
    void shouldPayInvoiceAndRegisterExitAndNotify() {
        Invoice inv = Invoice.builder().id(invoiceId).status(InvoiceStatus.ISSUED).order(order).number("INV-001").build();
        when(persistenceMethod.getInvoiceById(invoiceId)).thenReturn(inv);
        when(persistenceMethod.getOrderById(order.getId())).thenReturn(order);
        when(persistenceMethod.getUsersByRoleName(RoleList.ROLE_ADMIN)).thenReturn(List.of()); // admins/auditors lists
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("payer@example.com");
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(invoiceRepository.save(inv)).thenReturn(inv);

        ApiResponse<InvoiceDto> response = invoiceService.payInvoice(invoiceId);

        assertEquals(200, response.getCode());
        assertEquals("Invoice paid", response.getMessage());
        assertEquals(InvoiceStatus.PAID, inv.getStatus());
        verify(inventoryService, times(1)).registerInventoryExit(order.getId());
        verify(notificationService, atLeastOnce()).createNotification(anyString(), contains("has been paid"), any(UUID.class));
    }

    @Test
    void shouldThrowWhenPayingNonIssuedInvoice() {
        Invoice inv = Invoice.builder().id(invoiceId).status(InvoiceStatus.DRAFT).order(order).build();
        when(persistenceMethod.getInvoiceById(invoiceId)).thenReturn(inv);

        assertThrows(IllegalStateException.class, () -> invoiceService.payInvoice(invoiceId));
        verify(inventoryService, never()).registerInventoryExit(any());
    }

    @Test
    void shouldCancelInvoiceWhenNotPaid() {
        Invoice inv = Invoice.builder().id(invoiceId).status(InvoiceStatus.ISSUED).order(order).number("INV-002").build();
        when(persistenceMethod.getInvoiceById(invoiceId)).thenReturn(inv);
        when(persistenceMethod.getOrderById(order.getId())).thenReturn(order);
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(invoiceRepository.save(inv)).thenReturn(inv);

        ApiResponse<Void> response = invoiceService.cancelInvoice(invoiceId);

        assertEquals(200, response.getCode());
        assertEquals("Invoice canceled", response.getMessage());
        assertEquals(InvoiceStatus.CANCELED, inv.getStatus());
        assertEquals(OrderStatus.CANCELED, order.getStatus());
        verify(notificationService, times(1)).createNotification(eq("cancelInvoice"), contains("has been canceled"), eq(order.getUser().getId()));
    }

    @Test
    void shouldThrowWhenCancelingPaidInvoice() {
        Invoice paid = Invoice.builder().id(invoiceId).status(InvoiceStatus.PAID).order(order).build();
        when(persistenceMethod.getInvoiceById(invoiceId)).thenReturn(paid);

        assertThrows(IllegalStateException.class, () -> invoiceService.cancelInvoice(invoiceId));
    }
}