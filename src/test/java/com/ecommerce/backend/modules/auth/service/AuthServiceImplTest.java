package com.ecommerce.backend.modules.auth.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.exception.DuplicateResourceException;
import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.common.service.MailService;
import com.ecommerce.backend.modules.auth.dto.request.*;
import com.ecommerce.backend.modules.auth.dto.response.AuthResponse;
import com.ecommerce.backend.modules.auth.dto.response.MessageResponse;
import com.ecommerce.backend.modules.auth.entity.User;
import com.ecommerce.backend.modules.auth.enums.Role;
import com.ecommerce.backend.modules.auth.enums.UserStatus;
import com.ecommerce.backend.modules.auth.repository.UserRepository;
import com.ecommerce.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private MailService mailService;

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtDecoder jwtDecoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private User activeUser;
    private User pendingUser;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .id(1L)
                .email("active@example.com")
                .password("encoded_password")
                .status(UserStatus.ACTIVE)
                .role(Role.CUSTOMER)
                .build();

        pendingUser = User.builder()
                .id(2L)
                .email("pending@example.com")
                .password("encoded_password")
                .status(UserStatus.PENDING)
                .build();
    }

    // ==========================================
    // REGISTER TESTS
    // ==========================================

    @Test
    @DisplayName("Register: Should fail when passwords do not match")
    void register_Fail_PasswordMismatch() {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123", "password321");

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.register(request));
        assertThat(ex.getMessage()).isEqualTo("Passwords do not match");
    }

    @Test
    @DisplayName("Register: Should fail when email is already registered and active")
    void register_Fail_AlreadyRegistered() {
        RegisterRequest request = new RegisterRequest("active@example.com", "password", "password");
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(activeUser));

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () -> authService.register(request));
        assertThat(ex.getMessage()).isEqualTo("This email is already registered.");
    }

    @Test
    @DisplayName("Register: Should update and resend verification if user is pending")
    void register_Success_PendingUser() {
        RegisterRequest request = new RegisterRequest("pending@example.com", "new_password", "new_password");
        
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(pendingUser));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_new_password");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        MessageResponse response = authService.register(request);

        assertThat(response.getMessage()).contains("Registration updated");
        verify(userRepository, times(1)).save(pendingUser);
        verify(mailService, times(1)).sendVerificationEmail(eq("pending@example.com"), anyString());
    }

    @Test
    @DisplayName("Register: Should create new user successfully")
    void register_Success_NewUser() {
        RegisterRequest request = new RegisterRequest("new@example.com", "password", "password");
        
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        MessageResponse response = authService.register(request);

        assertThat(response.getMessage()).contains("Registration successful");
        verify(userRepository, times(1)).save(any(User.class));
        verify(mailService, times(1)).sendVerificationEmail(eq("new@example.com"), anyString());
    }

    // ==========================================
    // LOGIN TESTS
    // ==========================================

    @Test
    @DisplayName("Login: Should fail on invalid email")
    void login_Fail_InvalidEmail() {
        LoginRequest request = new LoginRequest("wrong@example.com", "password");
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> authService.login(request));
        assertThat(ex.getMessage()).isEqualTo("Invalid email or password.");
    }

    @Test
    @DisplayName("Login: Should fail on wrong password")
    void login_Fail_WrongPassword() {
        LoginRequest request = new LoginRequest("active@example.com", "wrong_password");
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrong_password", "encoded_password")).thenReturn(false);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.login(request));
        assertThat(ex.getMessage()).isEqualTo("Invalid email or password.");
    }

    @Test
    @DisplayName("Login: Should fail if user is pending")
    void login_Fail_PendingUser() {
        LoginRequest request = new LoginRequest("pending@example.com", "password");
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(pendingUser));
        when(passwordEncoder.matches("password", "encoded_password")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.login(request));
        assertThat(ex.getMessage()).contains("This account is not verified yet");
    }

    @Test
    @DisplayName("Login: Should return token on successful login")
    void login_Success() {
        LoginRequest request = new LoginRequest("active@example.com", "password");
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("password", "encoded_password")).thenReturn(true);
        when(jwtService.generateToken(activeUser)).thenReturn("mocked.jwt.token");

        AuthResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("mocked.jwt.token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUser().getEmail()).isEqualTo("active@example.com");
    }

    // ==========================================
    // VERIFY EMAIL TESTS
    // ==========================================

    @Test
    @DisplayName("Verify Email: Should fail on invalid/expired token")
    void verifyEmail_Fail_InvalidToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("verify:invalid-token")).thenReturn(null);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.verifyEmail("invalid-token"));
        assertThat(ex.getMessage()).contains("expired or is invalid");
    }

    @Test
    @DisplayName("Verify Email: Should activate user and return token")
    void verifyEmail_Success() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("verify:valid-token")).thenReturn("pending@example.com");
        when(userRepository.findByEmail("pending@example.com")).thenReturn(Optional.of(pendingUser));
        when(jwtService.generateToken(pendingUser)).thenReturn("new.jwt.token");

        AuthResponse response = authService.verifyEmail("valid-token");

        assertThat(pendingUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(userRepository, times(1)).save(pendingUser);
        verify(redisTemplate, times(1)).delete("verify:valid-token");
        assertThat(response.getAccessToken()).isEqualTo("new.jwt.token");
    }

    // ==========================================
    // LOGOUT TESTS
    // ==========================================

    @Test
    @DisplayName("Logout: Should blacklist valid token")
    void logout_Success() {
        String token = "Bearer valid.jwt.token";
        Jwt mockJwt = mock(Jwt.class);
        when(jwtDecoder.decode("valid.jwt.token")).thenReturn(mockJwt);
        when(mockJwt.getExpiresAt()).thenReturn(Instant.now().plusSeconds(3600)); // expires in 1 hour
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        authService.logout(token);

        verify(valueOperations, times(1)).set(eq("blacklist:valid.jwt.token"), eq("logged_out"), anyLong(), any());
    }

    // ==========================================
    // FORGOT PASSWORD TESTS
    // ==========================================

    @Test
    @DisplayName("Forgot Password: Should send reset email for active user")
    void forgotPassword_Success() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("active@example.com");
        when(userRepository.findByEmail("active@example.com")).thenReturn(Optional.of(activeUser));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        MessageResponse response = authService.forgotPassword(request);

        assertThat(response.getMessage()).contains("If the email is registered, a reset link has been sent");
        verify(valueOperations, times(1)).set(anyString(), eq("active@example.com"), eq(1L), any());
        verify(mailService, times(1)).sendPasswordResetEmail(eq("active@example.com"), anyString());
    }

    // ==========================================
    // RESET PASSWORD TESTS
    // ==========================================

    @Test
    @DisplayName("Reset Password: Should fail on password mismatch")
    void resetPassword_Fail_PasswordMismatch() {
        ResetPasswordRequest request = new ResetPasswordRequest("valid-token", "new_pass", "diff_pass");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("reset_password:valid-token")).thenReturn("active@example.com");
        when(userRepository.findByEmail("active@example.com")).thenReturn(Optional.of(activeUser));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> authService.resetPassword(request));
        assertThat(ex.getMessage()).isEqualTo("Passwords do not match.");
    }

    @Test
    @DisplayName("Reset Password: Should succeed and save new password")
    void resetPassword_Success() {
        ResetPasswordRequest request = new ResetPasswordRequest("valid-token", "new_pass", "new_pass");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("reset_password:valid-token")).thenReturn("active@example.com");
        when(userRepository.findByEmail("active@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.encode("new_pass")).thenReturn("encoded_new_pass");

        MessageResponse response = authService.resetPassword(request);

        assertThat(activeUser.getPassword()).isEqualTo("encoded_new_pass");
        verify(userRepository, times(1)).save(activeUser);
        verify(redisTemplate, times(1)).delete("reset_password:valid-token");
        assertThat(response.getMessage()).isEqualTo("Password reset successfully.");
    }
}
