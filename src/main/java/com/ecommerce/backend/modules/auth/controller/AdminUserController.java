package com.ecommerce.backend.modules.auth.controller;

import com.ecommerce.backend.modules.auth.dto.response.UserAdminResponse;
import com.ecommerce.backend.modules.auth.enums.Role;
import com.ecommerce.backend.modules.auth.enums.UserStatus;
import com.ecommerce.backend.modules.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<Page<UserAdminResponse>> getUsers(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<UserAdminResponse> response = userService.getAdminUsers(keyword, pageable);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<String> changeUserStatus (@PathVariable Long id, @RequestParam UserStatus status) {
        userService.changeUserStatus(id, status);

        String message = status == UserStatus.INACTIVE
                ? "User account locked successfully."
                : "User account unlocked successfully.";

        return ResponseEntity.ok(message);
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> changeUserRole(@PathVariable Long id, @RequestParam Role role) {
        userService.changeUserRole(id, role);
        return ResponseEntity.ok("User role updated successfully to " + role + ".");
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserAdminResponse> getUserDetails(@PathVariable Long id) {
        UserAdminResponse response = userService.getUserDetails(id);

        return ResponseEntity.ok(response);
    }
}
