package com.sigi.services.service.invoice;

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
import com.sigi.util.DtoMapper;
import com.sigi.util.PersistenceMethod;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static com.sigi.util.Constants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final DtoMapper dtoMapper;
    private final MeterRegistry meterRegistry;
    private final PersistenceMethod persistenceMethod;
    private final NotificationService notificationService;
    private final InventoryService inventoryService;

    @Override
    @Transactional
    @CacheEvict(value = {INVOICE_BY_ID, ALL_ACTIVE_INVOICES, INVOICES_BY_CLIENT, INVOICES_BY_STATUS, INVOICE_BY_NUMBER, INVENTORIES_DELETED_BY_NAME, ALL_DELETED_INVOICES, ORDER_BY_ID, ORDER_CALCULATE, ORDERS_BY_CLIENT, ORDERS_BY_USER, ALL_ACTIVE_ORDERS, ALL_DELETED_ORDERS}, allEntries = true)
    public ApiResponse<InvoiceDto> createInvoice(NewInvoiceDto dto) {
        log.debug("(createInvoice) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Order order = persistenceMethod.getOrderById(dto.getOrderId());
        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new IllegalStateException("Only draft invoices can be create");
        }
        BigDecimal subtotal = order.getTotal();
        BigDecimal taxValue = subtotal.multiply(dto.getTax()).divide(BigDecimal.valueOf(100));
        BigDecimal total = subtotal.add(taxValue);
        String number = "INV-" + System.currentTimeMillis();
        Invoice saved = invoiceRepository.save(Invoice.builder()
                .number(number)
                .order(order)
                .client(order.getClient())
                .subtotal(subtotal)
                .tax(taxValue)
                .total(total)
                .status(InvoiceStatus.ISSUED)
                .active(true)
                .createdAt(LocalDateTime.now(ZoneId.of("America/Bogota")))
                .createdBy(persistenceMethod.getCurrentUserEmail())
                .build());
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
        recordMetrics(sample, "createInvoice");
        log.info("(createInvoice) -> " + OPERATION_COMPLETED);
        return ApiResponse.success("Invoice created", dtoMapper.toInvoiceDto(saved));
    }

    @Override
    @CacheEvict(value = {INVOICE_BY_ID, ALL_ACTIVE_INVOICES, INVOICES_BY_CLIENT, INVOICES_BY_STATUS, INVOICE_BY_NUMBER, INVENTORIES_DELETED_BY_NAME, ALL_DELETED_INVOICES}, allEntries = true)
    @Transactional
    public ApiResponse<InvoiceDto> updateInvoice(UUID id, NewInvoiceDto dto) {
        log.debug("(updateInvoice) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Invoice invoice = persistenceMethod.getInvoiceById(id);
        if (invoice.getStatus() != InvoiceStatus.ISSUED) {
            throw new IllegalStateException("Only issued invoices can be updated");
        }
        Order order = persistenceMethod.getOrderById(dto.getOrderId());
        BigDecimal subtotal = order.getTotal();
        BigDecimal taxValue = subtotal.multiply(dto.getTax()).divide(BigDecimal.valueOf(100));
        BigDecimal total = subtotal.add(taxValue);
        invoice.setOrder(order);
        invoice.setClient(order.getClient());
        invoice.setSubtotal(subtotal);
        invoice.setTax(taxValue);
        invoice.setTotal(total);
        invoice.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        invoice.setUpdatedBy(persistenceMethod.getCurrentUserEmail());
        invoiceRepository.save(invoice);
        recordMetrics(sample, "updateInvoice");
        log.info("(updateInvoice) -> " + OPERATION_COMPLETED);
        return ApiResponse.success("Invoice updated", dtoMapper.toInvoiceDto(invoice));
    }

    @Override
    @CacheEvict(value = {INVOICE_BY_ID, ALL_ACTIVE_INVOICES, INVOICES_BY_CLIENT, INVOICES_BY_STATUS, INVOICE_BY_NUMBER, INVENTORIES_DELETED_BY_NAME, ALL_DELETED_INVOICES}, allEntries = true)
    public ApiResponse<Void> deleteInvoice(UUID id) {
        log.debug("(deleteInvoice) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Invoice invoice = persistenceMethod.getInvoiceById(id);
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Paid invoices cannot be deleted");
        }
        invoice.setActive(false);
        invoice.setDeletedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        invoice.setDeletedBy(persistenceMethod.getCurrentUserEmail());
        invoiceRepository.save(invoice);
        recordMetrics(sample, "deleteInvoice");
        log.info("(deleteInvoice) -> " + OPERATION_COMPLETED);
        return ApiResponse.success("Invoice deleted", null);
    }

    @Override
    @CacheEvict(value = {INVOICE_BY_ID, ALL_ACTIVE_INVOICES, INVOICES_BY_CLIENT, INVOICES_BY_STATUS, INVOICE_BY_NUMBER, INVENTORIES_DELETED_BY_NAME, ALL_DELETED_INVOICES}, allEntries = true)
    public ApiResponse<Void> restoreInvoice(UUID id) {
        log.debug("(restoreInvoice) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Invoice invoice = persistenceMethod.getInvoiceById(id);
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Paid invoices cannot be restored");
        }
        invoice.setActive(true);
        invoice.setDeletedAt(null);
        invoice.setDeletedBy(null);
        invoiceRepository.save(invoice);
        recordMetrics(sample, "restoreInvoice");
        log.info("(restoreInvoice) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(ENTITY_ACTIVATED_SUCCESSFULLY.formatted(INVOICE), null);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = INVOICE_BY_ID, key = "#id")
    public ApiResponse<InvoiceDto> getInvoiceById(UUID id) {
        log.debug("(getInvoiceById) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Invoice response = persistenceMethod.getInvoiceById(id);
        if (response.getActive().equals(Boolean.FALSE)) {
            throw new IllegalArgumentException(ENTITY_INACTIVE.formatted(INVOICE_ID.formatted(id)));
        }
        recordMetrics(sample, "getInvoiceById");
        log.info("(getInvoiceById) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(SEARCH_SUCCESSFULLY.formatted(INVOICE),
                dtoMapper.toInvoiceDto(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = INVOICE_BY_ID, key = "#id")
    public ApiResponse<InvoiceDto> getDeletedInvoiceById(UUID id) {
        log.debug("(getDeletedInvoiceById) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Invoice response = invoiceRepository.findByIdAndActiveFalse(id).orElseThrow(() -> new EntityNotFoundException(ENTITY_NOT_FOUND.formatted(INVOICE_ID.formatted(id))));
        recordMetrics(sample, "getDeletedInvoiceById");
        log.info("(getDeletedInvoiceById) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(SEARCH_SUCCESSFULLY.formatted(INVOICE),
                dtoMapper.toInvoiceDto(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ALL_ACTIVE_INVOICES, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<InvoiceDto>> listAllActiveInvoices(PagedRequestDto pagedRequestDto) {
        log.debug("(listAllActiveInvoices) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Invoice> response = invoiceRepository.findByActiveTrue(PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        recordMetrics(sample, "listAllActiveInvoices");
        log.info("(listAllActiveInvoices) -> " + OPERATION_COMPLETED);
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(INVOICE),
                dtoMapper.toInvoiceDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ALL_DELETED_INVOICES, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<InvoiceDto>> listAllDeletedInvoices(PagedRequestDto pagedRequestDto) {
        log.debug("(listAllDeletedInvoices) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Invoice> response = invoiceRepository.findByActiveFalse(PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        recordMetrics(sample, "listAllDeletedInvoices");
        log.info("(listAllDeletedInvoices) -> " + OPERATION_COMPLETED);
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(INVOICE),
                dtoMapper.toInvoiceDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = INVOICE_BY_NUMBER, key = "#number")
    public ApiResponse<InvoiceDto> getInvoiceByNumber(String number) {
        log.debug("(getInvoiceByNumber) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Invoice response = persistenceMethod.getInvoiceByNumber(number);
        if (response.getActive().equals(Boolean.FALSE)) {
            throw new IllegalArgumentException(ENTITY_INACTIVE.formatted(INVOICE_ID.formatted(response.getId())));
        }
        recordMetrics(sample, "getInvoiceByNumber");
        log.info("(getInvoiceByNumber) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(SEARCH_SUCCESSFULLY.formatted(INVOICE),
                dtoMapper.toInvoiceDto(response));
    }

    @Override
    public ApiResponse<InvoiceDto> getInvoiceByOrder(UUID orderId) {
        log.debug("(getInvoiceByOrder) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Invoice response = invoiceRepository.findByOrderId(orderId).orElseThrow(() -> new EntityNotFoundException(ENTITY_NOT_FOUND.formatted(INVOICE)));
        if (response.getActive().equals(Boolean.FALSE)) {
            throw new IllegalArgumentException(ENTITY_INACTIVE.formatted(INVOICE_ID.formatted(response.getId())));
        }
        recordMetrics(sample, "getInvoiceByOrder");
        log.info("(getInvoiceByOrder) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(SEARCH_SUCCESSFULLY.formatted(INVOICE),
                dtoMapper.toInvoiceDto(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = INVOICES_BY_CLIENT, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<InvoiceDto>> listInvoicesByClient(PagedRequestDto pagedRequestDto) {
        log.debug("(listInvoicesByClient) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Invoice> response = invoiceRepository.findByClientNameContainingIgnoreCaseAndActiveTrue(pagedRequestDto.getSearchValue(), PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        recordMetrics(sample, "listInvoicesByClient");
        log.info("(listInvoicesByClient) -> " + OPERATION_COMPLETED);
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(INVOICE),
                dtoMapper.toInvoiceDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = INVOICES_BY_STATUS, key = "{#status,#pagedRequestDto}")
    public ApiResponse<PagedResponse<InvoiceDto>> listInvoicesByStatus(InvoiceStatus status, PagedRequestDto pagedRequestDto) {
        log.debug("(listInvoicesByStatus) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Invoice> response = invoiceRepository.findByStatusAndActiveTrue(status, PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        recordMetrics(sample, "listInvoicesByStatus");
        log.info("(listInvoicesByStatus) -> " + OPERATION_COMPLETED);
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(INVOICE),
                dtoMapper.toInvoiceDtoPage(response));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = INVOICES_DELETED_BY_NAME, key = "#pagedRequestDto")
    @Override
    public ApiResponse<PagedResponse<InvoiceDto>> listDeletedInvoicesByNumber(PagedRequestDto pagedRequestDto) {
        log.debug("(listDeletedInvoicesByNumber) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Invoice> response = invoiceRepository.findByNumberContainingIgnoreCaseAndActiveTrue(pagedRequestDto.getSearchValue(), PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        recordMetrics(sample, "listDeletedInvoicesByNumber");
        log.info("(listDeletedInvoicesByNumber) -> " + OPERATION_COMPLETED);
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(INVOICE),
                dtoMapper.toInvoiceDtoPage(response));
    }

    @Override
    @Transactional
    @CacheEvict(value = {INVOICE_BY_ID, ALL_ACTIVE_INVOICES, INVOICES_BY_CLIENT, INVOICES_BY_STATUS, INVOICE_BY_NUMBER, INVENTORIES_DELETED_BY_NAME, ALL_DELETED_INVOICES}, allEntries = true)
    public ApiResponse<InvoiceDto> issueInvoice(UUID id) {
        log.debug("(issueInvoice) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Invoice invoice = persistenceMethod.getInvoiceById(id);
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Only draft invoices can be issued");
        }
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        invoice.setUpdatedBy(persistenceMethod.getCurrentUserEmail());
        invoiceRepository.save(invoice);
        notificationService.createNotification(
                "Invoice Issued",
                "Invoice %s has been issued.".formatted(invoice.getNumber()),
                invoice.getOrder().getUser().getId()
        );
        recordMetrics(sample, "issueInvoice");
        log.info("(issueInvoice) -> " + OPERATION_COMPLETED);
        return ApiResponse.success("Invoice issued", dtoMapper.toInvoiceDto(invoice));
    }

    @Override
    @Transactional
    @CacheEvict(value = {
            INVOICE_BY_ID, ALL_ACTIVE_INVOICES, ALL_ACTIVE_ORDERS,
            INVOICES_BY_CLIENT, INVOICES_BY_STATUS, INVOICE_BY_NUMBER,
            INVENTORIES_DELETED_BY_NAME, ALL_DELETED_INVOICES
    }, allEntries = true)
    public ApiResponse<InvoiceDto> payInvoice(UUID id) {
        log.debug("(payInvoice) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);

        Invoice invoice = persistenceMethod.getInvoiceById(id);
        if (invoice.getStatus() != InvoiceStatus.ISSUED) {
            throw new IllegalStateException("Only issued invoices can be paid");
        }

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        invoice.setUpdatedBy(persistenceMethod.getCurrentUserEmail());
        invoiceRepository.save(invoice);

        Order order = persistenceMethod.getOrderById(invoice.getOrder().getId());
        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);

        inventoryService.registerInventoryExit(order.getId());

        notificationService.createNotification(
                "payInvoice",
                "Invoice %s has been paid.".formatted(invoice.getNumber()),
                order.getUser().getId()
        );

        List<User> admins = persistenceMethod.getUsersByRoleName(RoleList.ROLE_ADMIN);
        List<User> auditors = persistenceMethod.getUsersByRoleName(RoleList.ROLE_AUDITOR);
        admins.forEach(admin -> notificationService.createNotification(
                "payInvoice",
                "Invoice %s has been paid.".formatted(invoice.getNumber()),
                admin.getId()
        ));
        auditors.forEach(auditor -> notificationService.createNotification(
                "payInvoice",
                "Invoice %s has been paid.".formatted(invoice.getNumber()),
                auditor.getId()
        ));

        recordMetrics(sample, "payInvoice");
        log.info("(payInvoice) -> " + OPERATION_COMPLETED);
        return ApiResponse.success("Invoice paid", dtoMapper.toInvoiceDto(invoice));
    }

    @Override
    @Transactional
    @CacheEvict(value = {INVOICE_BY_ID, ALL_ACTIVE_INVOICES, INVOICES_BY_CLIENT, INVOICES_BY_STATUS, INVOICE_BY_NUMBER, INVENTORIES_DELETED_BY_NAME, ALL_DELETED_INVOICES}, allEntries = true)
    public ApiResponse<Void> cancelInvoice(UUID id) {
        log.debug("(cancelInvoice) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Invoice invoice = persistenceMethod.getInvoiceById(id);
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Paid invoices cannot be canceled");
        }
        invoice.setStatus(InvoiceStatus.CANCELED);
        invoice.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        invoice.setUpdatedBy(persistenceMethod.getCurrentUserEmail());
        invoiceRepository.save(invoice);
        Order order = persistenceMethod.getOrderById(invoice.getOrder().getId());
        order.setStatus(OrderStatus.CANCELED);
        orderRepository.save(order);
        notificationService.createNotification(
                "cancelInvoice",
                "Invoice %s has been canceled.".formatted(invoice.getNumber()),
                invoice.getOrder().getUser().getId()
        );
        recordMetrics(sample, "cancelInvoice");
        log.info("(cancelInvoice) -> " + OPERATION_COMPLETED);
        return ApiResponse.success("Invoice canceled", null);
    }


    private void recordMetrics(Timer.Sample sample, String operation) {
        sample.stop(Timer.builder("invoice.service.latency")
                .tag("operation", operation)
                .register(meterRegistry));
        meterRegistry.counter("invoice.service.operations", "type", operation).increment();
    }
}
