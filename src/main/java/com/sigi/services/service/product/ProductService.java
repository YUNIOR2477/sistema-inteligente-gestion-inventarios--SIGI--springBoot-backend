package com.sigi.services.service.product;

import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.product.NewProductDto;
import com.sigi.presentation.dto.product.ProductDto;
import com.sigi.presentation.dto.response.PagedResponse;

import java.math.BigDecimal;
import java.util.UUID;

public interface ProductService {
    ApiResponse<ProductDto> createProduct(NewProductDto dto);

    ApiResponse<ProductDto> updateProduct(UUID id, NewProductDto dto);

    ApiResponse<Void> deleteProduct(UUID id);

    ApiResponse<Void> restoreProduct(UUID id);

    ApiResponse<ProductDto> getProductById(UUID id);

    ApiResponse<ProductDto> getDeletedProductById(UUID id);

    ApiResponse<PagedResponse<ProductDto>> listAllActiveProducts(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<ProductDto>> listAllDeletedProducts(PagedRequestDto pagedRequestDto);

    ApiResponse<ProductDto> getProductBySku(String sku);

    ApiResponse<PagedResponse<ProductDto>> listProductsByName(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<ProductDto>> listDeletedProductsByName(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<ProductDto>> listProductsByCategory(PagedRequestDto pagedRequestDto);

    ApiResponse<PagedResponse<ProductDto>> listProductsByPriceRange(BigDecimal min, BigDecimal max, PagedRequestDto pagedRequestDto);
}

