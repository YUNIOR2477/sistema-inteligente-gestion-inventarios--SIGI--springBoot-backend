package com.sigi.services.service.order.line;

import com.sigi.presentation.dto.order.NewOrderLineDto;
import com.sigi.presentation.dto.order.OrderLineDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;

import java.util.UUID;

public interface OrderLineService {
    ApiResponse<OrderLineDto> createOrderLine(UUID orderId, NewOrderLineDto dto);

    ApiResponse<OrderLineDto> updateOrderLine(UUID id, NewOrderLineDto dto);

    ApiResponse<Void> deleteOrderLine(UUID id);

    ApiResponse<Void> restoreOrderLine(UUID id);

    ApiResponse<OrderLineDto> getOrderLineById(UUID id);

    ApiResponse<OrderLineDto> getDeletedOrderLineById(UUID id);

    ApiResponse<PagedResponse<OrderLineDto>> listAllActiveLines(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<OrderLineDto>> listAllDeletedLines(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<OrderLineDto>> listLinesByOrder(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<OrderLineDto>> listActiveLinesByProductName(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<OrderLineDto>> listDeletedLinesByProductName(PagedRequestDto pagedRequestDto);
}
