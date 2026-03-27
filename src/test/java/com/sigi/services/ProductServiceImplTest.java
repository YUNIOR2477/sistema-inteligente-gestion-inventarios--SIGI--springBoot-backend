package com.sigi.services;

import com.sigi.persistence.entity.Product;
import com.sigi.persistence.repository.InventoryRepository;
import com.sigi.persistence.repository.ProductRepository;
import com.sigi.presentation.dto.product.NewProductDto;
import com.sigi.presentation.dto.product.ProductDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.presentation.dto.response.PagedResponse;
import com.sigi.services.service.product.ProductServiceImpl;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.sigi.util.Constants.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private DtoMapper dtoMapper;

    @Mock
    private PersistenceMethod persistenceMethod;

    @InjectMocks
    private ProductServiceImpl productService;

    private SimpleMeterRegistry meterRegistry;

    private UUID productId;
    private Product product;
    private ProductDto productDto;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        productService = new ProductServiceImpl(productRepository, inventoryRepository, dtoMapper, meterRegistry, persistenceMethod);

        productId = UUID.randomUUID();
        product = Product.builder()
                .id(productId)
                .sku("SKU-001")
                .name("Test Product")
                .category("Cat")
                .unit("piece")
                .price(BigDecimal.valueOf(100))
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        productDto = ProductDto.builder()
                .id(productId)
                .sku(product.getSku())
                .name(product.getName())
                .price(product.getPrice())
                .active(product.getActive())
                .build();

        lenient().when(dtoMapper.toProductDto(any(Product.class))).thenReturn(productDto);
        lenient().when(dtoMapper.toProductDtoPage(any(Page.class))).thenAnswer(inv -> {
            Page<Product> page = inv.getArgument(0);
            return page.map(p -> productDto);
        });
    }

    // ------------------- createProduct -------------------
    @Test
    void shouldCreateProductSuccessfully() {
        NewProductDto dto = NewProductDto.builder()
                .sku("SKU-NEW")
                .name("New Product")
                .category("Cat")
                .unit("piece")
                .price(BigDecimal.valueOf(50))
                .build();

        when(productRepository.existsBySku(dto.getSku())).thenReturn(false);
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("creator@example.com");
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(productId);
            return p;
        });

        ApiResponse<ProductDto> response = productService.createProduct(dto);

        assertEquals(200, response.getCode());
        assertEquals(CREATED_SUCCESSFULLY.formatted(PRODUCT), response.getMessage());
        verify(productRepository, times(1)).save(any(Product.class));
        assertTrue(meterRegistry.get("product.service.operations").tags("type", "createProduct").counter().count() >= 1.0);
    }

    @Test
    void shouldThrowWhenCreateProductSkuExistsAndActive() {
        NewProductDto dto = NewProductDto.builder().sku("SKU-EX").name("X").category("C").unit("u").price(BigDecimal.ONE).build();
        Product existing = Product.builder().id(UUID.randomUUID()).sku("SKU-EX").active(true).build();

        when(productRepository.existsBySku(dto.getSku())).thenReturn(true);
        when(persistenceMethod.getProductBySku(dto.getSku())).thenReturn(existing);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> productService.createProduct(dto));
        assertTrue(ex.getMessage().contains("already"));
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenCreateProductSkuExistsButInactive() {
        NewProductDto dto = NewProductDto.builder().sku("SKU-EX").name("X").category("C").unit("u").price(BigDecimal.ONE).build();
        Product existing = Product.builder().id(UUID.randomUUID()).sku("SKU-EX").active(false).build();

        when(productRepository.existsBySku(dto.getSku())).thenReturn(true);
        when(persistenceMethod.getProductBySku(dto.getSku())).thenReturn(existing);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> productService.createProduct(dto));
        assertTrue(ex.getMessage().toLowerCase().contains("inactive"));
        verify(productRepository, never()).save(any());
    }

    // ------------------- updateProduct -------------------
    @Test
    void shouldUpdateProductSuccessfully() {
        NewProductDto dto = NewProductDto.builder()
                .sku("SKU-001")
                .name("Updated")
                .category("Cat2")
                .unit("box")
                .price(BigDecimal.valueOf(120))
                .build();

        Product existing = Product.builder().id(productId).active(true).price(BigDecimal.valueOf(90)).build();
        when(persistenceMethod.getProductById(productId)).thenReturn(existing);
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("updater@example.com");
        when(productRepository.save(existing)).thenReturn(existing);

        ApiResponse<ProductDto> response = productService.updateProduct(productId, dto);

        assertEquals(200, response.getCode());
        assertEquals(UPDATED_SUCCESSFULLY.formatted(PRODUCT), response.getMessage());
        verify(productRepository, times(1)).save(existing);
        assertNotNull(existing.getUpdatedAt());
        assertEquals("updater@example.com", existing.getUpdatedBy());
    }

    @Test
    void shouldThrowWhenUpdateProductInactive() {
        NewProductDto dto = NewProductDto.builder().sku("SKU-001").name("X").category("C").unit("u").price(BigDecimal.ONE).build();
        Product existing = Product.builder().id(productId).active(false).build();
        when(persistenceMethod.getProductById(productId)).thenReturn(existing);

        assertThrows(IllegalArgumentException.class, () -> productService.updateProduct(productId, dto));
        verify(productRepository, never()).save(any());
    }

    // ------------------- deleteProduct -------------------
    @Test
    void shouldDeleteProductSuccessfullyWhenNoInventory() {
        Product existing = Product.builder().id(productId).active(true).build();
        when(persistenceMethod.getProductById(productId)).thenReturn(existing);
        when(inventoryRepository.existsByProductIdAndActiveTrue(productId)).thenReturn(false);
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("deleter@example.com");
        when(productRepository.save(existing)).thenReturn(existing);

        ApiResponse<Void> response = productService.deleteProduct(productId);

        assertEquals(200, response.getCode());
        assertEquals(DELETED_SUCCESSFULLY.formatted(PRODUCT), response.getMessage());
        assertFalse(existing.getActive());
        assertNotNull(existing.getDeletedAt());
        assertEquals("deleter@example.com", existing.getDeletedBy());
    }

    @Test
    void shouldThrowWhenDeleteProductHasActiveInventory() {
        Product existing = Product.builder().id(productId).active(true).build();
        when(persistenceMethod.getProductById(productId)).thenReturn(existing);
        when(inventoryRepository.existsByProductIdAndActiveTrue(productId)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> productService.deleteProduct(productId));
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenDeleteProductAlreadyInactive() {
        Product existing = Product.builder().id(productId).active(false).build();
        when(persistenceMethod.getProductById(productId)).thenReturn(existing);

        assertThrows(IllegalArgumentException.class, () -> productService.deleteProduct(productId));
        verify(productRepository, never()).save(any());
    }

    // ------------------- restoreProduct -------------------
    @Test
    void shouldRestoreProductSuccessfully() {
        Product existing = Product.builder().id(productId).active(false).sku("SKU-REST").build();
        when(persistenceMethod.getProductById(productId)).thenReturn(existing);
        when(productRepository.existsBySkuAndActiveTrue(existing.getSku())).thenReturn(false);
        when(persistenceMethod.getCurrentUserEmail()).thenReturn("restorer@example.com");
        when(productRepository.save(existing)).thenReturn(existing);

        ApiResponse<Void> response = productService.restoreProduct(productId);

        assertEquals(200, response.getCode());
        assertTrue(existing.getActive());
        assertNull(existing.getDeletedAt());
        assertNull(existing.getDeletedBy());
    }

    @Test
    void shouldThrowWhenRestoreProductAlreadyActive() {
        Product existing = Product.builder().id(productId).active(true).build();
        when(persistenceMethod.getProductById(productId)).thenReturn(existing);

        assertThrows(IllegalArgumentException.class, () -> productService.restoreProduct(productId));
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenRestoreProductSkuConflict() {
        Product existing = Product.builder().id(productId).active(false).sku("SKU-CONF").build();
        when(persistenceMethod.getProductById(productId)).thenReturn(existing);
        when(productRepository.existsBySkuAndActiveTrue(existing.getSku())).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> productService.restoreProduct(productId));
    }

    // ------------------- getProductById / getDeletedProductById -------------------
    @Test
    void shouldGetProductByIdWhenActive() {
        Product active = Product.builder().id(productId).active(true).build();
        when(persistenceMethod.getProductById(productId)).thenReturn(active);

        ApiResponse<ProductDto> response = productService.getProductById(productId);

        assertEquals(200, response.getCode());
        assertEquals(productDto, response.getData());
    }

    @Test
    void shouldThrowWhenGetProductByIdIfInactive() {
        Product inactive = Product.builder().id(productId).active(false).build();
        when(persistenceMethod.getProductById(productId)).thenReturn(inactive);

        assertThrows(IllegalArgumentException.class, () -> productService.getProductById(productId));
    }

    @Test
    void shouldGetDeletedProductByIdSuccessfully() {
        Product deleted = Product.builder().id(productId).active(false).build();
        when(productRepository.findByIdAndActiveFalse(productId)).thenReturn(Optional.of(deleted));

        ApiResponse<ProductDto> response = productService.getDeletedProductById(productId);

        assertEquals(200, response.getCode());
        assertEquals(productDto, response.getData());
    }

    @Test
    void shouldThrowWhenDeletedProductNotFound() {
        when(productRepository.findByIdAndActiveFalse(productId)).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> productService.getDeletedProductById(productId));
    }

    // ------------------- list paginados / getBySku / search -------------------
    @Test
    void shouldListAllActiveProductsSuccessfully() {
        PagedRequestDto req = PagedRequestDto.builder().page(0).size(10).sortDirection("desc").sortField("createdAt").build();
        Page<Product> page = new PageImpl<>(List.of(product), PageRequest.of(0,10), 1);
        when(productRepository.findByActiveTrue(any(Pageable.class))).thenReturn(page);

        ApiResponse<PagedResponse<ProductDto>> response = productService.listAllActiveProducts(req);

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().getTotalElements());
    }

    @Test
    void shouldGetProductBySkuSuccessfullyWhenActive() {
        when(persistenceMethod.getProductBySku("SKU-001")).thenReturn(product);

        ApiResponse<ProductDto> response = productService.getProductBySku("SKU-001");

        assertEquals(200, response.getCode());
        assertEquals(productDto, response.getData());
    }

    @Test
    void shouldThrowWhenGetProductBySkuIfInactive() {
        Product inactive = Product.builder().id(productId).sku("SKU-001").active(false).build();
        when(persistenceMethod.getProductBySku("SKU-001")).thenReturn(inactive);

        assertThrows(IllegalArgumentException.class, () -> productService.getProductBySku("SKU-001"));
    }

    @Test
    void shouldListProductsByNameSuccessfully() {
        PagedRequestDto req = PagedRequestDto.builder().page(0).size(10).searchValue("Test").sortDirection("desc").sortField("createdAt").build();
        Page<Product> page = new PageImpl<>(List.of(product));
        when(productRepository.findByNameContainingIgnoreCaseAndActiveTrue(eq("Test"), any(Pageable.class))).thenReturn(page);

        ApiResponse<PagedResponse<ProductDto>> response = productService.listProductsByName(req);

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().getTotalElements());
    }

    @Test
    void shouldListProductsByPriceRangeSuccessfully() {
        PagedRequestDto req = PagedRequestDto.builder().page(0).size(10).sortDirection("desc").sortField("createdAt").build();
        Page<Product> page = new PageImpl<>(List.of(product));
        when(productRepository.findByPriceRangeAndActiveTrue(eq(BigDecimal.ZERO), eq(BigDecimal.valueOf(200)), any(Pageable.class))).thenReturn(page);

        ApiResponse<PagedResponse<ProductDto>> response = productService.listProductsByPriceRange(BigDecimal.ZERO, BigDecimal.valueOf(200), req);

        assertEquals(200, response.getCode());
        assertEquals(1, response.getData().getTotalElements());
    }
}