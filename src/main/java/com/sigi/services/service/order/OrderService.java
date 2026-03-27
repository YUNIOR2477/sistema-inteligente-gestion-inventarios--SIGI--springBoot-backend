package com.sigi.services.service.order;

import com.sigi.presentation.dto.order.NewOrderDto;
import com.sigi.presentation.dto.order.OrderDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;

import java.util.UUID;

public interface OrderService {
    ApiResponse<OrderDto> createOrder(NewOrderDto dto);

    ApiResponse<OrderDto> updateOrder(UUID id, NewOrderDto dto);

    ApiResponse<Void> deleteOrder(UUID id);

    ApiResponse<Void> restoreOrder(UUID id);

    ApiResponse<OrderDto> getOrderById(UUID id);

    ApiResponse<OrderDto> getDeletedOrderById(UUID id);

    ApiResponse<PagedResponse<OrderDto>> listAllActiveOrders(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<OrderDto>> listAllDeletedOrders(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<OrderDto>> listOrdersByClient(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<OrderDto>> listOrdersByClientName(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<OrderDto>> listOrdersByUser(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<OrderDto>> listOrdersByInventory(PagedRequestDto pagedRequestDto);

    ApiResponse<Void> cancelOrder(UUID id);

    ApiResponse<Void> changeOrderStatus(UUID id, String status);

}
