package com.sigi.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigi.configuration.DataInitializer;
import com.sigi.integration.config.TestDataInitializer;
import com.sigi.presentation.dto.auth.LoginUserDto;
import com.sigi.presentation.dto.client.NewClientDto;
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

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ImportAutoConfiguration(exclude = DataInitializer.class)
@Import(TestDataInitializer.class)
class ClientEndToEndTest {

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
    void fullClientFlow() throws Exception {
        NewClientDto newClient = NewClientDto.builder()
                .name("Cliente Test")
                .email("cliente@test.com")
                .phone("3001234567")
                .identification("1234567890")
                .location("Armenia")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/clients")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newClient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Client has been created successfully.")))
                .andReturn();

        String createJson = createResult.getResponse().getContentAsString();
        JsonNode createNode = objectMapper.readTree(createJson);
        String clientId = createNode.get("data").get("id").asText();

        mockMvc.perform(get("/api/v1/clients/" + clientId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Cliente Test")))
                .andExpect(jsonPath("$.data.email", is("cliente@test.com")));
    }
}
