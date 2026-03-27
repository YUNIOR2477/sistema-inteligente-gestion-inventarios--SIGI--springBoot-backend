package com.sigi.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigi.configuration.DataInitializer;
import com.sigi.integration.config.TestDataInitializer;
import com.sigi.presentation.dto.auth.LoginUserDto;
import com.sigi.presentation.dto.dispatcher.NewDispatcherDto;
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

import static com.sigi.util.Constants.CREATED_SUCCESSFULLY;
import static com.sigi.util.Constants.DISPATCHER;
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
class DispatcherEndToEndTest {
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
    void fullDispatcherFlow() throws Exception {
        NewDispatcherDto newDispatcher = NewDispatcherDto.builder()
                .name("Dispatcher Test")
                .email("dispatcher@test.com")
                .phone("3012345678")
                .identification("987654321")
                .location("Armenia")
                .contact("Contacto Test")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/dispatchers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newDispatcher)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(CREATED_SUCCESSFULLY.formatted(DISPATCHER))))
                .andReturn();

        String createJson = createResult.getResponse().getContentAsString();
        JsonNode createNode = objectMapper.readTree(createJson);
        String dispatcherId = createNode.get("data").get("id").asText();

        mockMvc.perform(get("/api/v1/dispatchers/" + dispatcherId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name", is("Dispatcher Test")))
                .andExpect(jsonPath("$.data.email", is("dispatcher@test.com")))
                .andExpect(jsonPath("$.data.identification", is("987654321")));
    }

}
