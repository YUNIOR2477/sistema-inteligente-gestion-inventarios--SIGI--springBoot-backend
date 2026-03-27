package com.sigi.services;


import com.sigi.persistence.entity.Role;
import com.sigi.persistence.entity.User;
import com.sigi.persistence.enums.RoleList;
import com.sigi.persistence.repository.UserRepository;
import com.sigi.services.service.auth.AuthUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthUserService authUserService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(UUID.randomUUID())
                .email("test@mail.com")
                .password("password123")
                .role(Role.builder().name(RoleList.ROLE_ADMIN).build())
                .build();
    }

    @Test
    void shouldLoadUserByUsernameSuccessfully() {
        // Arrange
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = authUserService.loadUserByUsername(user.getEmail());

        // Assert
        assertEquals(user.getEmail(), userDetails.getUsername());
        assertEquals(user.getPassword(), userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        // Arrange
        when(userRepository.findByEmail("notfound@mail.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class,
                () -> authUserService.loadUserByUsername("notfound@mail.com"));
    }

    @Test
    void shouldReturnTrueWhenUserExists() {
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(true);
        assertTrue(authUserService.existsByUserName(user.getEmail()));
    }

    @Test
    void shouldReturnFalseWhenUserDoesNotExist() {
        when(userRepository.existsByEmail("notfound@mail.com")).thenReturn(false);
        assertFalse(authUserService.existsByUserName("notfound@mail.com"));
    }

    @Test
    void shouldSaveUserSuccessfully() {
        authUserService.save(user);
        verify(userRepository, times(1)).save(user);
    }
}

