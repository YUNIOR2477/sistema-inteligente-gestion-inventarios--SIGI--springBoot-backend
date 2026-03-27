package com.sigi.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigi.presentation.dto.product.NewProductDto;
import com.sigi.presentation.dto.product.ProductDto;
import com.sigi.presentation.dto.request.PagedRequestDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.services.service.product.ProductService;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ProductController using MockMvc (standalone) and Mockito.
 * Focus: routing, request binding, response shape and service interaction.
 */
@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private UUID productId;
    private ProductDto productDto;
    private NewProductDto newProductDto;

    @Captor
    private ArgumentCaptor<PagedRequestDto> pagedRequestCaptor;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController).build();

        productId = UUID.randomUUID();
        productDto = ProductDto.builder()
                .id(productId)
                .sku("SKU-123")
                .name("Test Product")
                .category("Electronics")
                .unit("piece")
                .price(BigDecimal.valueOf(199.99))
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        newProductDto = NewProductDto.builder()
                .sku("SKU-123")
                .name("Test Product")
                .category("Electronics")
                .unit("piece")
                .price(BigDecimal.valueOf(199.99))
                .barcode("7701234567890")
                .imageUrl("https://example.com/image.jpg")
                .build();
    }

    @Test
    void createProduct_delegatesToService_andReturnsCreatedDto() throws Exception {
        when(productService.createProduct(any(NewProductDto.class)))
                .thenReturn(ApiResponse.success("Created", productDto));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newProductDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(productId.toString()))
                .andExpect(jsonPath("$.data.sku").value("SKU-123"));

        verify(productService, times(1)).createProduct(any(NewProductDto.class));
    }

    @Test
    void updateProduct_delegatesToService_andReturnsUpdatedDto() throws Exception {
        ProductDto updated = ProductDto.builder()
                .id(productId)
                .sku("SKU-123")
                .name("Updated Product")
                .category("Electronics")
                .unit("piece")
                .price(BigDecimal.valueOf(249.99))
                .build();

        when(productService.updateProduct(eq(productId), any(NewProductDto.class)))
                .thenReturn(ApiResponse.success("Updated", updated));

        mockMvc.perform(put("/api/v1/products/{id}", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newProductDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(productId.toString()))
                .andExpect(jsonPath("$.data.name").value("Updated Product"))
                .andExpect(jsonPath("$.data.price").value(249.99));

        verify(productService, times(1)).updateProduct(eq(productId), any(NewProductDto.class));
    }

    @Test
    void deleteProduct_callsService_andReturnsOk() throws Exception {
        when(productService.deleteProduct(productId)).thenReturn(ApiResponse.success("Deleted", null));

        mockMvc.perform(delete("/api/v1/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Deleted"));

        verify(productService, times(1)).deleteProduct(productId);
    }

    @Test
    void restoreProduct_callsService_andReturnsOk() throws Exception {
        when(productService.restoreProduct(productId)).thenReturn(ApiResponse.success("Restored", null));

        mockMvc.perform(put("/api/v1/products/{id}/restore", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Restored"));

        verify(productService, times(1)).restoreProduct(productId);
    }

    @Test
    void getProductById_returnsProduct() throws Exception {
        when(productService.getProductById(productId)).thenReturn(ApiResponse.success("Found", productDto));

        mockMvc.perform(get("/api/v1/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(productId.toString()))
                .andExpect(jsonPath("$.code").value(200));

        verify(productService, times(1)).getProductById(productId);
    }

    @Test
    void getDeletedProductById_callsService() throws Exception {
        when(productService.getDeletedProductById(productId)).thenReturn(ApiResponse.success("Found", productDto));

        mockMvc.perform(get("/api/v1/products/deleted/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(productId.toString()));

        verify(productService, times(1)).getDeletedProductById(productId);
    }

    @Test
    void listAllActiveProducts_bindsPagedRequest_andCallsService() throws Exception {
        when(productService.listAllActiveProducts(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/products")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortField", "createdAt")
                        .param("sortDirection", "desc"))
                .andExpect(status().isOk());

        verify(productService, times(1)).listAllActiveProducts(pagedRequestCaptor.capture());
        PagedRequestDto captured = pagedRequestCaptor.getValue();
        assertThat(captured.getPage()).isEqualTo(0);
        assertThat(captured.getSize()).isEqualTo(10);
        assertThat(captured.getSortField()).isEqualTo("createdAt");
    }

    @Test
    void listAllDeletedProducts_callsService() throws Exception {
        when(productService.listAllDeletedProducts(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/products/deleted")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(productService, times(1)).listAllDeletedProducts(any(PagedRequestDto.class));
    }

    @Test
    void getProductBySku_callsService_andReturnsProduct() throws Exception {
        when(productService.getProductBySku("SKU-123")).thenReturn(ApiResponse.success("Found", productDto));

        mockMvc.perform(get("/api/v1/products/by-sku")
                        .param("sku", "SKU-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sku").value("SKU-123"));

        verify(productService, times(1)).getProductBySku("SKU-123");
    }

    @Test
    void listProductsByName_passesPagedRequest_andCallsService() throws Exception {
        when(productService.listProductsByName(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/products/by-name")
                        .param("searchValue", "Test")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(productService, times(1)).listProductsByName(pagedRequestCaptor.capture());
        PagedRequestDto captured = pagedRequestCaptor.getValue();
        assertThat(captured.getSearchValue()).isEqualTo("Test");
        assertThat(captured.getSize()).isEqualTo(5);
    }

    @Test
    void listProductsByCategory_callsService() throws Exception {
        when(productService.listProductsByCategory(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/products/by-category")
                        .param("searchValue", "Electronics")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(productService, times(1)).listProductsByCategory(any(PagedRequestDto.class));
    }

    @Test
    void listProductsByPriceRange_callsService_andBindsParams() throws Exception {
        when(productService.listProductsByPriceRange(any(BigDecimal.class), any(BigDecimal.class), any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/products/by-price-range")
                        .param("min", "10.00")
                        .param("max", "500.00")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk());

        verify(productService, times(1)).listProductsByPriceRange(eq(new BigDecimal("10.00")), eq(new BigDecimal("500.00")), any(PagedRequestDto.class));
    }

    @Test
    void listDeletedProductsByName_callsService() throws Exception {
        when(productService.listDeletedProductsByName(any(PagedRequestDto.class)))
                .thenReturn(ApiResponse.success("OK", null));

        mockMvc.perform(get("/api/v1/products/deleted/by-name")
                        .param("searchValue", "Old")
                        .param("page", "0")
                        .param("size", "3"))
                .andExpect(status().isOk());

        verify(productService, times(1)).listDeletedProductsByName(any(PagedRequestDto.class));
    }
}