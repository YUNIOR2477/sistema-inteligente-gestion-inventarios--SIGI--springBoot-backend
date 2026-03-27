package com.sigi.services;

import com.sigi.configuration.security.jwt.JwtUtil;
import com.sigi.persistence.entity.User;
import com.sigi.persistence.repository.UserRepository;
import com.sigi.presentation.dto.auth.LoginUserDto;
import com.sigi.presentation.dto.response.ApiResponse;
import com.sigi.services.service.auth.AuthService;
import com.sigi.services.service.auth.AuthUserService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static com.sigi.util.Constants.INVALID_CREDENTIALS;
import static com.sigi.util.Constants.SUCCESSFUL_LOGIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private AuthUserService authUserService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManagerBuilder authenticationManagerBuilder;

    @Mock
    private Authentication authentication;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AuthService authService;

    private LoginUserDto loginDto;
    private User user;

    @BeforeEach
    void setUp() {
        loginDto = new LoginUserDto("test@mail.com", "password123");
        user = User.builder()
                .id(UUID.randomUUID())
                .email("test@mail.com")
                .password("password123")
                .build();
    }

    @Test
    void shouldAuthenticateSuccessfully() {
        // Arrange
        when(authenticationManagerBuilder.getObject()).thenReturn(auth -> authentication);
        when(userRepository.findByEmail(loginDto.getEmail())).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken(authentication, request)).thenReturn("jwt-token");

        // Act
        ApiResponse<String> response = authService.authenticate(loginDto, request);

        // Assert
        assertEquals(HttpStatus.OK.value(), response.getCode());
        assertEquals(SUCCESSFUL_LOGIN, response.getMessage());
        assertEquals("jwt-token", response.getData());
        verify(authUserService, times(1)).save(user);
    }

    @Test
    void shouldThrowEntityNotFoundWhenEmailNotExists() {
        // Arrange
        when(authenticationManagerBuilder.getObject()).thenReturn(auth -> authentication);
        when(userRepository.findByEmail(loginDto.getEmail())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class,
                () -> authService.authenticate(loginDto, request));
    }

    @Test
    void shouldThrowUnauthorizedWhenBadCredentials() {
        // Arrange
        when(authenticationManagerBuilder.getObject()).thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> authService.authenticate(loginDto, request));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        assertEquals(INVALID_CREDENTIALS, ex.getReason());
    }
}
