package com.sigi.services.service.product;

import com.sigi.persistence.entity.Product;
import com.sigi.persistence.repository.InventoryRepository;
import com.sigi.persistence.repository.ProductRepository;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.product.NewProductDto;
import com.sigi.presentation.dto.product.ProductDto;
import com.sigi.presentation.dto.response.PagedResponse;
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

import static com.sigi.util.Constants.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final DtoMapper dtoMapper;
    private final MeterRegistry meterRegistry;
    private final PersistenceMethod persistenceMethod;

    @Override
    @Transactional
    @CacheEvict(value = {PRODUCT_BY_ID, PRODUCT_BY_SKU, PRODUCTS_BY_NAME, PRODUCTS_BY_CATEGORY, PRODUCTS_BY_PRICE_RANGE, ALL_ACTIVE_PRODUCTS, ALL_DELETED_PRODUCTS, PRODUCTS_DELETED_BY_NAME}, allEntries = true)
    public ApiResponse<ProductDto> createProduct(NewProductDto dto) {
        log.debug("(createProduct) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        if (productRepository.existsBySku(dto.getSku())) {
            Product existingProduct = persistenceMethod.getProductBySku(dto.getSku());
            if (Boolean.TRUE.equals(existingProduct.getActive())) {
                throw new IllegalArgumentException(ENTITY_ALREADY_EXISTS.formatted(PRODUCT_SKU.formatted(dto.getSku())));
            } else
                throw new IllegalArgumentException(ENTITY_INACTIVE.formatted(PRODUCT_SKU.formatted(dto.getSku())));
        }
        Product p = Product.builder()
                .sku(dto.getSku())
                .name(dto.getName())
                .category(dto.getCategory())
                .unit(dto.getUnit())
                .price(dto.getPrice() == null ? BigDecimal.ZERO : dto.getPrice())
                .barcode(dto.getBarcode())
                .imageUrl(dto.getImageUrl())
                .active(true)
                .createdAt(LocalDateTime.now(ZoneId.of("America/Bogota")))
                .createdBy(persistenceMethod.getCurrentUserEmail())
                .build();
        Product saved = productRepository.save(p);
        recordMetrics(sample, "createProduct");
        log.info("(createProduct) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(CREATED_SUCCESSFULLY.formatted(PRODUCT),
                dtoMapper.toProductDto(saved));
    }

    @Override
    @Transactional
    @CacheEvict(value = {PRODUCT_BY_ID, PRODUCT_BY_SKU, PRODUCTS_BY_NAME, PRODUCTS_BY_CATEGORY, PRODUCTS_BY_PRICE_RANGE, ALL_ACTIVE_PRODUCTS, ALL_DELETED_PRODUCTS, PRODUCTS_DELETED_BY_NAME}, allEntries = true)
    public ApiResponse<ProductDto> updateProduct(UUID id, NewProductDto dto) {
        log.debug("(updateProduct) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Product existing = persistenceMethod.getProductById(id);
        if (existing.getActive().equals(Boolean.FALSE)) {
            throw new IllegalArgumentException(ENTITY_INACTIVE.formatted(PRODUCT_ID.formatted(id)));
        }
        existing.setSku(dto.getSku());
        existing.setName(dto.getName());
        existing.setCategory(dto.getCategory());
        existing.setUnit(dto.getUnit());
        existing.setPrice(dto.getPrice() == null ? existing.getPrice() : dto.getPrice());
        existing.setBarcode(dto.getBarcode());
        existing.setImageUrl(dto.getImageUrl());
        existing.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        existing.setUpdatedBy(persistenceMethod.getCurrentUserEmail());
        Product updated = productRepository.save(existing);
        recordMetrics(sample, "updateProduct");
        log.info("(updateProduct) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(UPDATED_SUCCESSFULLY.formatted(PRODUCT),
                dtoMapper.toProductDto(updated));
    }

    @Override
    @Transactional
    @CacheEvict(value = {PRODUCT_BY_ID, PRODUCT_BY_SKU, PRODUCTS_BY_NAME, PRODUCTS_BY_CATEGORY, PRODUCTS_BY_PRICE_RANGE, ALL_ACTIVE_PRODUCTS, ALL_DELETED_PRODUCTS, PRODUCTS_DELETED_BY_NAME}, allEntries = true)
    public ApiResponse<Void> deleteProduct(UUID id) {
        log.debug("(deleteProduct) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Product existingProduct = persistenceMethod.getProductById(id);
        if (existingProduct.getActive().equals(Boolean.FALSE)) {
            throw new IllegalArgumentException(ENTITY_PREVIOUSLY_DELETED.formatted(PRODUCT_ID.formatted(id)));
        }
        boolean hasInventory = inventoryRepository.existsByProductIdAndActiveTrue(id);
        if (hasInventory) {
            throw new IllegalStateException("Cannot delete product with active inventory");
        }
        existingProduct.setActive(false);
        existingProduct.setDeletedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        existingProduct.setDeletedBy(persistenceMethod.getCurrentUserEmail());
        productRepository.save(existingProduct);
        recordMetrics(sample, "deleteProduct");
        log.info("(deleteProduct) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(DELETED_SUCCESSFULLY.formatted(PRODUCT), null);
    }

    @Override
    @Transactional
    @CacheEvict(value = {PRODUCT_BY_ID, PRODUCT_BY_SKU, PRODUCTS_BY_NAME, PRODUCTS_BY_CATEGORY, PRODUCTS_BY_PRICE_RANGE, ALL_ACTIVE_PRODUCTS, ALL_DELETED_PRODUCTS, PRODUCTS_DELETED_BY_NAME}, allEntries = true)
    public ApiResponse<Void> restoreProduct(UUID id) {
        log.debug("(restoreProduct) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Product existingProduct = persistenceMethod.getProductById(id);
        if (Boolean.TRUE.equals(existingProduct.getActive())) {
            throw new IllegalArgumentException(ENTITY_ALREADY_ACTIVE.formatted(PRODUCT_ID.formatted(id)));
        }
        if (productRepository.existsBySkuAndActiveTrue(existingProduct.getSku())) {
            throw new IllegalStateException("Cannot restore product: active product with same SKU exists");
        }
        existingProduct.setActive(true);
        existingProduct.setDeletedAt(null);
        existingProduct.setDeletedBy(null);
        existingProduct.setUpdatedAt(LocalDateTime.now(ZoneId.of("America/Bogota")));
        existingProduct.setUpdatedBy(persistenceMethod.getCurrentUserEmail());
        productRepository.save(existingProduct);
        recordMetrics(sample, "restoreProduct");
        log.info("(restoreProduct) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(ENTITY_ACTIVATED_SUCCESSFULLY.formatted(PRODUCT_ID.formatted(id)), null);
    }


    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = PRODUCT_BY_ID, key = "#id")
    public ApiResponse<ProductDto> getProductById(UUID id) {
        log.debug("(getProductById) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Product response = persistenceMethod.getProductById(id);
        if (response.getActive().equals(Boolean.FALSE)) {
            throw new IllegalArgumentException(ENTITY_INACTIVE.formatted(PRODUCT_ID.formatted(id)));
        }
        recordMetrics(sample, "getProductById");
        log.info("(getProductById) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(SEARCH_SUCCESSFULLY.formatted(PRODUCT),
                dtoMapper.toProductDto(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = PRODUCT_BY_ID, key = "#id")
    public ApiResponse<ProductDto> getDeletedProductById(UUID id) {
        log.debug("(getDeletedProductById) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Product response = productRepository.findByIdAndActiveFalse(id).orElseThrow(() -> new EntityNotFoundException(ENTITY_NOT_FOUND.formatted(PRODUCT_ID.formatted(id))));
        recordMetrics(sample, "getDeletedProductById");
        log.info("(getDeletedProductById) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(SEARCH_SUCCESSFULLY.formatted(ORDER),
                dtoMapper.toProductDto(response));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = ALL_ACTIVE_PRODUCTS, key = "#pagedRequestDto")
    @Override
    public ApiResponse<PagedResponse<ProductDto>> listAllActiveProducts(PagedRequestDto pagedRequestDto) {
        log.debug("(listAllActiveProducts) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Product> response = productRepository.findByActiveTrue(PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        recordMetrics(sample, "listClientsByName");
        log.info("(listAllActiveProducts) -> " + OPERATION_COMPLETED);
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(PRODUCTS),
                dtoMapper.toProductDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ALL_DELETED_PRODUCTS, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<ProductDto>> listAllDeletedProducts(PagedRequestDto pagedRequestDto) {
        log.debug("(listAllDeletedProducts) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Product> response = productRepository.findByActiveFalse(PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        recordMetrics(sample, "listAllDeletedProducts");
        log.info("(listAllDeletedProducts) -> " + OPERATION_COMPLETED);
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(PRODUCTS),
                dtoMapper.toProductDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = PRODUCT_BY_SKU, key = "#sku")
    public ApiResponse<ProductDto> getProductBySku(String sku) {
        log.debug("(getProductBySku) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Product existingProduct = persistenceMethod.getProductBySku(sku);
        if (existingProduct.getActive().equals(Boolean.FALSE)) {
            throw new IllegalArgumentException(ENTITY_INACTIVE.formatted(PRODUCT_SKU.formatted(sku)));
        }
        recordMetrics(sample, "getProductBySku");
        log.info("(getProductBySku) -> " + OPERATION_COMPLETED);
        return ApiResponse.success(SEARCH_SUCCESSFULLY.formatted(PRODUCT),
                dtoMapper.toProductDto(existingProduct));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = PRODUCTS_BY_NAME, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<ProductDto>> listProductsByName(PagedRequestDto pagedRequestDto) {
        log.debug("(listClientsByName) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Product> response = productRepository.findByNameContainingIgnoreCaseAndActiveTrue(pagedRequestDto.getSearchValue(), PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        recordMetrics(sample, "listClientsByName");
        log.info("(listClientsByName) -> " + OPERATION_COMPLETED);
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(PRODUCTS),
                dtoMapper.toProductDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = PRODUCTS_DELETED_BY_NAME, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<ProductDto>> listDeletedProductsByName(PagedRequestDto pagedRequestDto) {
        log.debug("(listDeletedProductsByName) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Product> response = productRepository.findByNameContainingIgnoreCaseAndActiveFalse(pagedRequestDto.getSearchValue(), PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        recordMetrics(sample, "listDeletedProductsByName");
        log.info("(listDeletedProductsByName) -> " + OPERATION_COMPLETED);
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(PRODUCTS),
                dtoMapper.toProductDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = PRODUCTS_BY_CATEGORY, key = "#pagedRequestDto")
    public ApiResponse<PagedResponse<ProductDto>> listProductsByCategory(PagedRequestDto pagedRequestDto) {
        log.debug("(listProductsByCategory) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Product> response = productRepository.findByCategoryContainingIgnoreCaseAndActiveTrue(pagedRequestDto.getSearchValue(), PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        recordMetrics(sample, "listProductsByCategory");
        log.info("(listProductsByCategory) -> " + OPERATION_COMPLETED);
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(PRODUCTS),
                dtoMapper.toProductDtoPage(response));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = PRODUCTS_BY_PRICE_RANGE, key = "{#min, #max,#pagedRequestDto}")
    public ApiResponse<PagedResponse<ProductDto>> listProductsByPriceRange(BigDecimal min, BigDecimal max, PagedRequestDto pagedRequestDto) {
        log.debug("(listProductsByPriceRange) -> " + PERFORMING_OPERATION);
        Timer.Sample sample = Timer.start(meterRegistry);
        Page<Product> response = productRepository.findByPriceRangeAndActiveTrue(min, max, PageRequest.of(pagedRequestDto.getPage(), pagedRequestDto.getSize(), Sort.by(Sort.Direction.fromString(pagedRequestDto.getSortDirection()), pagedRequestDto.getSortField())));
        recordMetrics(sample, "listProductsByPriceRange");
        log.info("(listProductsByPriceRange) -> " + OPERATION_COMPLETED);
        return ApiResponse.successPage(SEARCH_SUCCESSFULLY.formatted(PRODUCTS),
                dtoMapper.toProductDtoPage(response));
    }

    private void recordMetrics(Timer.Sample sample, String operation) {
        sample.stop(Timer.builder("product.service.latency")
                .tag("operation", operation)
                .register(meterRegistry));
        meterRegistry.counter("product.service.operations", "type", operation).increment();
    }

}
