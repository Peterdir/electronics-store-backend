package com.ecommerce.backend.modules.auth.controller;

import com.ecommerce.backend.modules.auth.dto.request.ChangePasswordRequest;
import com.ecommerce.backend.modules.auth.dto.request.UpdateProfileRequest;
import com.ecommerce.backend.modules.auth.dto.response.MessageResponse;
import com.ecommerce.backend.modules.auth.dto.response.UserProfileResponse;
import com.ecommerce.backend.modules.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.valueOf(Objects.requireNonNull(jwt.getSubject(), "JWT subject must not be null"));
        return ResponseEntity.ok(userService.getUserProfile(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<MessageResponse> updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequest request) {
        Long userId = Long.valueOf(Objects.requireNonNull(jwt.getSubject(), "JWT subject must not be null"));
        userService.updateUserProfile(userId, request);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Profile updated successfully.")
                .build());
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@RequestBody @Valid ChangePasswordRequest request, Principal principal) {
        String email = principal.getName();

        userService.changePassword(email, request);

        return ResponseEntity.ok("Password changed successfully.");
    }
}
