package com.ecommerce.backend.modules.auth.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.modules.auth.dto.request.LoginRequest;
import com.ecommerce.backend.modules.auth.dto.response.AuthResponse;
import com.ecommerce.backend.modules.auth.entity.User;
import com.ecommerce.backend.modules.auth.enums.UserStatus;
import com.ecommerce.backend.modules.auth.repository.UserRepository;
import com.ecommerce.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User mockUser;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("user@example.com");
        mockUser.setPassword("hashedPassword");
        mockUser.setStatus(UserStatus.ACTIVE);

        loginRequest = new LoginRequest();
        loginRequest.setEmail("user@example.com");
        loginRequest.setPassword("plainPassword");
    }

    @Test
    void login_Success() {
        // Arrange
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("plainPassword", "hashedPassword")).thenReturn(true);
        when(jwtService.generateToken(any(User.class))).thenReturn("fake-jwt-token");

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertEquals("fake-jwt-token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
    }

    @Test
    void login_UserNotFound() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            authService.login(loginRequest);
        });
    }

    @Test
    void login_InvalidPassword() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            authService.login(loginRequest);
        });
    }

    @Test
    void login_UserPending() {
        // Arrange
        mockUser.setStatus(UserStatus.PENDING);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            authService.login(loginRequest);
        });
    }

    @Test
    void login_UserInactive() {
        // Arrange
        mockUser.setStatus(UserStatus.INACTIVE);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            authService.login(loginRequest);
        });
    }
}
