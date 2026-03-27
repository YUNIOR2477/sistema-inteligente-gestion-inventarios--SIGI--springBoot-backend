package com.sigi.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigi.configuration.DataInitializer;
import com.sigi.integration.config.TestDataInitializer;
import com.sigi.presentation.dto.auth.LoginUserDto;
import com.sigi.presentation.dto.client.NewClientDto;
import com.sigi.presentation.dto.dispatcher.NewDispatcherDto;
import com.sigi.presentation.dto.order.NewOrderDto;
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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ImportAutoConfiguration(exclude = DataInitializer.class)
@Import(TestDataInitializer.class)
class OrderEndToEndTest {

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
    void fullOrderFlow() throws Exception {
        // client
        NewClientDto newClient = NewClientDto.builder()
                .name("Cliente Test")
                .email("cliente12@test.com")
                .phone("302201234567")
                .identification("234567890")
                .location("Armenia")
                .build();

        MvcResult createResultClient = mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newClient)))
                .andExpect(status().isOk())
                .andReturn();

        String createJsonClient = createResultClient.getResponse().getContentAsString();
        JsonNode createNodeClient = objectMapper.readTree(createJsonClient);
        String clientId = createNodeClient.get("data").get("id").asText();

        // user
        MvcResult createResultUser = mockMvc.perform(get("/api/v1/users/by-email")
                        .header("Authorization", "Bearer " + token)
                        .param("email", "admin@sigi.com"))
                .andExpect(status().isOk())
                .andReturn();

        String createJsonUser = createResultUser.getResponse().getContentAsString();
        JsonNode createNodeUser = objectMapper.readTree(createJsonUser);
        String userId = createNodeUser.get("data").get("id").asText();

        // warehouse
        NewWarehouseDto newWarehouse = new NewWarehouseDto("Main Warehouse31", "Location A11", 100);

        MvcResult createResultWarehouse = mockMvc.perform(post("/api/v1/warehouses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newWarehouse)))
                .andExpect(status().isOk())
                .andReturn();

        String createJsonClientWarehouse = createResultWarehouse.getResponse().getContentAsString();
        JsonNode createNodeWarehouse = objectMapper.readTree(createJsonClientWarehouse);
        String warehouseId = createNodeWarehouse.get("data").get("id").asText();

        // dispatcher
        NewDispatcherDto newDispatcherDto = new NewDispatcherDto("Dispatcher2","1111121","Colombia","2121223122","dispatcher2@sigi.com","dispatcher2");

        MvcResult createResultDispatcher = mockMvc.perform(post("/api/v1/dispatchers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newDispatcherDto)))
                .andExpect(status().isOk())
                .andReturn();

        String createJsonClientDispatcher = createResultDispatcher.getResponse().getContentAsString();
        JsonNode createNodeDispatcher = objectMapper.readTree(createJsonClientDispatcher);
        String dispatcherId = createNodeDispatcher.get("data").get("id").asText();

        // order
        NewOrderDto newOrder = NewOrderDto.builder()
                .clientId(UUID.fromString(clientId))
                .warehouseId(UUID.fromString(warehouseId))
                .dispatcherId(UUID.fromString(dispatcherId) )
                .build();

        MvcResult orderResultOrder = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newOrder)))
                .andExpect(status().isOk())
                .andReturn();

        String orderJson = orderResultOrder.getResponse().getContentAsString();
        JsonNode orderNode = objectMapper.readTree(orderJson);
        String orderId = orderNode.get("data").get("id").asText();


        mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
