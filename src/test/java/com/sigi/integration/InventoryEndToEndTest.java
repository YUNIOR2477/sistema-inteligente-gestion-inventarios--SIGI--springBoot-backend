package com.sigi.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigi.configuration.DataInitializer;
import com.sigi.integration.config.TestDataInitializer;
import com.sigi.presentation.dto.auth.LoginUserDto;
import com.sigi.presentation.dto.inventory.NewInventoryDto;
import com.sigi.presentation.dto.product.NewProductDto;
import com.sigi.presentation.dto.warehouse.NewWarehouseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ImportAutoConfiguration(exclude = DataInitializer.class)
@Import(TestDataInitializer.class)
class InventoryEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void loginAdmin() throws Exception {
        LoginUserDto loginRequest = new LoginUserDto("admin@sigi.com", "123456789");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = loginResult.getResponse().getContentAsString();
        JsonNode jsonNode = objectMapper.readTree(responseJson);
        token = jsonNode.get("data").asText();
    }

    @Test
    void fullInventoryFlow() throws Exception {
        // warehouse
        NewWarehouseDto newWarehouse = new NewWarehouseDto("Main Warehouse", "Location A", 100);

        MvcResult createResultWarehouse = mockMvc.perform(post("/api/v1/warehouses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newWarehouse)))
                .andExpect(status().isOk())
                .andReturn();

        String createJsonClientWarehouse = createResultWarehouse.getResponse().getContentAsString();
        JsonNode createNodeWarehouse = objectMapper.readTree(createJsonClientWarehouse);
        String warehouseId = createNodeWarehouse.get("data").get("id").asText();

        // Product
        NewProductDto newProduct = NewProductDto.builder().sku("SKU-0031").name("Arroz Diana 500g").category("Alimentos").unit("kg")
                .price(BigDecimal.valueOf(2500)).barcode("77012345647890").imageUrl("https://example.com/arroz.jpg").build();
        MvcResult createResultProduct = mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newProduct)))
                .andExpect(status().isOk())
                .andReturn();

        String createJsonClientProduct = createResultProduct.getResponse().getContentAsString();
        JsonNode createNodeProduct = objectMapper.readTree(createJsonClientProduct);
        String productId = createNodeProduct.get("data").get("id").asText();

        // Inventory
        NewInventoryDto newInventory = NewInventoryDto.builder().productId(UUID.fromString(productId)).warehouseId(UUID.fromString(warehouseId)).location("Estante A1").lot("LOT-ARZ-2026")
                .productionDate(LocalDate.of(2025, 12, 1)).expirationDate(LocalDate.of(2026, 12, 1))
                .availableQuantity(BigDecimal.valueOf(500)).reservedQuantity(BigDecimal.valueOf(50)).build();
        MvcResult createResultInventory = mockMvc.perform(post("/api/v1/inventories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newInventory)))
                .andExpect(status().isOk())
                .andReturn();

        String createJsonClientInventory = createResultInventory.getResponse().getContentAsString();
        JsonNode createNodeInventory = objectMapper.readTree(createJsonClientInventory);
        String inventoryId = createNodeInventory.get("data").get("id").asText();

        mockMvc.perform(get("/api/v1/inventories/" + inventoryId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
