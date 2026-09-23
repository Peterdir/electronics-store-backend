package com.ecommerce.backend.modules.auth.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.exception.DuplicateResourceException;
import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.common.service.MailService;
import com.ecommerce.backend.modules.auth.dto.request.LoginRequest;
import com.ecommerce.backend.modules.auth.dto.request.RegisterRequest;
import com.ecommerce.backend.modules.auth.dto.request.ResendVerificationRequest;
import com.ecommerce.backend.modules.auth.dto.response.AuthResponse;
import com.ecommerce.backend.modules.auth.dto.response.MessageResponse;
import com.ecommerce.backend.modules.auth.dto.response.UserResponse;
import com.ecommerce.backend.modules.auth.entity.User;
import com.ecommerce.backend.modules.auth.enums.UserStatus;
import com.ecommerce.backend.modules.auth.repository.UserRepository;
import com.ecommerce.backend.security.JwtService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final MailService mailService;
    private final JwtService jwtService;
    private final JwtDecoder jwtDecoder;

    private static final String VERIFY_TOKEN_PREFIX = "verify:";
    private static final long TOKEN_EXPIRY_HOURS = 24;

    @Override
    @Transactional
    public MessageResponse register(RegisterRequest request) {

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        Optional<User> existingUser = userRepository.findByEmail(request.getEmail());

        if (existingUser.isPresent()) {
            User user = existingUser.get();

            if (user.getStatus() == UserStatus.PENDING) {
                throw new DuplicateResourceException("This account is pending verification.");
            }
            else {
                throw new DuplicateResourceException("This email is already registered.");
            }
        }

        User newUser = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.PENDING)
                .build();
        userRepository.save(newUser);

        sendVerificationToken(newUser.getEmail());

        return MessageResponse.builder()
                .message("Registration successful. Please check your email to verify your account.")
                .build();
    }

    @Override
    public MessageResponse resendVerification(ResendVerificationRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new BadRequestException("This account is already verified.");
        }

        sendVerificationToken(user.getEmail());

        return MessageResponse.builder()
                .message("Verification email has been resent. Please check your email.")
                .build();
    }

    @Override
    @Transactional
    public AuthResponse verifyEmail(String token) {
        String redisKey = VERIFY_TOKEN_PREFIX + token;
        String email = redisTemplate.opsForValue().get(redisKey);

        if (email == null) {
            throw new BadRequestException("This link has expired or is invalid.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);

        redisTemplate.delete(redisKey);

        String accessToken = jwtService.generateToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid email or password."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Invalid email or password.");
        }

        if (user.getStatus() == UserStatus.PENDING) {
            throw new BadRequestException("This account is not verified yet. Please check your email.");
        }

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new BadRequestException("This account has been locked. Please contact support");
        }

        String token = jwtService.generateToken(user);

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .status(user.getStatus())
                .build();

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .user(userResponse)
                .build();
    }

    @Override
    public void logout(String token) {
        try {

            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            Jwt jwt = jwtDecoder.decode(token);
            Instant expiresAt = jwt.getExpiresAt();

            if (expiresAt != null) {
                long timeToLive = Duration.between(Instant.now(), expiresAt).toMillis();

                if (timeToLive > 0) {
                    redisTemplate.opsForValue().set(
                            "blacklist:" + token,
                            "logged_out",
                            timeToLive,
                            TimeUnit.MILLISECONDS
                    );
                }
            }
        } catch (JwtException e) {
            log.debug("Logout called with invalid or expired token: {}", e.getMessage());
        }
    }

    private void sendVerificationToken(String email) {
        String token = UUID.randomUUID().toString();
        String redisKey = VERIFY_TOKEN_PREFIX + token;

        // Lưu vào Redis với TTL = 24 giờ
        redisTemplate.opsForValue().set(redisKey, email, TOKEN_EXPIRY_HOURS, TimeUnit.HOURS);

        mailService.sendVerificationEmail(email, token);
    }

}
