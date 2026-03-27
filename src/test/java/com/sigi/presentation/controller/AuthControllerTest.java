package com.sigi.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sigi.configuration.security.jwt.JwtUtil;
import com.sigi.presentation.controller.AuthController;
import com.sigi.presentation.dto.auth.LoginUserDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.services.service.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class,
        excludeAutoConfiguration = {org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class})

class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;


    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldAuthenticateUserSuccessfully() throws Exception {
        // Arrange
        LoginUserDto loginDto = new LoginUserDto("test@mail.com", "password123");
        ApiResponse<String> response = ApiResponse.success("Login completed successfully.", "jwt-token-123");

        Mockito.when(authService.authenticate(any(LoginUserDto.class), any(MockHttpServletRequest.class)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Login completed successfully.")))
                .andExpect(jsonPath("$.data", is("jwt-token-123")));
    }

    @Test
    void shouldReturnBadRequestWhenPasswordTooShort() throws Exception {
        LoginUserDto loginDto = new LoginUserDto("wrong@mail.com", "badpass"); // < 8 chars

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(400)))
                .andExpect(jsonPath("$.message", is("A validation error has occurred. Please review the provided information.")))
                .andExpect(jsonPath("$.errors[0]", is("Password must be at least 8 characters long")));
    }


    @Test
    void shouldReturnUnauthorizedWhenAuthFails() throws Exception {
        LoginUserDto loginDto = new LoginUserDto("wrong@mail.com", "invalidPassword123"); // válido en longitud

        ApiResponse<String> response = ApiResponse.error(401, "Authentication failed. Please verify your credentials.", null);

        Mockito.when(authService.authenticate(eq(loginDto), any(HttpServletRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is(401)))
                .andExpect(jsonPath("$.message", is("Authentication failed. Please verify your credentials.")));
    }
}