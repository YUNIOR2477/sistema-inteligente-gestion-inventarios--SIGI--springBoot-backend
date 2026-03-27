package com.sigi.services.service.invoice;

import com.sigi.persistence.enums.InvoiceStatus;
import com.sigi.presentation.dto.invoice.InvoiceDto;
import com.sigi.presentation.dto.invoice.NewInvoiceDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;

import java.util.UUID;

public interface InvoiceService {
    ApiResponse<InvoiceDto> createInvoice(NewInvoiceDto dto);

    ApiResponse<InvoiceDto> updateInvoice(UUID id, NewInvoiceDto dto);

    ApiResponse<Void> deleteInvoice(UUID id);

    ApiResponse<Void> restoreInvoice(UUID id);

    ApiResponse<InvoiceDto> getInvoiceById(UUID id);

    ApiResponse<InvoiceDto> getDeletedInvoiceById(UUID id);

    ApiResponse<PagedResponse<InvoiceDto>> listAllActiveInvoices(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<InvoiceDto>> listAllDeletedInvoices(PagedRequestDto pagedRequestDto);

    ApiResponse<InvoiceDto> getInvoiceByNumber(String number);

    ApiResponse<InvoiceDto> getInvoiceByOrder(UUID orderId);

    ApiResponse<PagedResponse<InvoiceDto>> listInvoicesByClient(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<InvoiceDto>> listInvoicesByStatus( InvoiceStatus status, PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<InvoiceDto>> listDeletedInvoicesByNumber(PagedRequestDto pagedRequestDto);

    ApiResponse<InvoiceDto> issueInvoice(UUID id);

    ApiResponse<InvoiceDto> payInvoice(UUID id);

    ApiResponse<Void> cancelInvoice(UUID id);
}
