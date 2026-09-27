package com.ecommerce.backend.modules.address.controller;

import com.ecommerce.backend.modules.address.dto.request.AddressRequest;
import com.ecommerce.backend.modules.address.dto.response.AddressResponse;
import com.ecommerce.backend.modules.address.service.AddressService;
import com.ecommerce.backend.modules.auth.dto.response.MessageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    private Long extractUserId(Jwt jwt) {
        return Long.valueOf(Objects.requireNonNull(jwt.getSubject(), "JWT subject must not be null"));
    }

    @GetMapping
    public ResponseEntity<List<AddressResponse>> getMyAddresses(@AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserId(jwt);
        return ResponseEntity.ok(addressService.getMyAddresses(userId));
    }

    @PostMapping
    public ResponseEntity<MessageResponse> addAddress(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddressRequest request) {
        Long userId = extractUserId(jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(addressService.addAddress(userId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MessageResponse> updateAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long addressId,
            @Valid @RequestBody AddressRequest request) {
        Long userId = extractUserId(jwt);
        return ResponseEntity.ok(addressService.updateAddress(userId, addressId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long addressId) {
        Long userId = extractUserId(jwt);
        return ResponseEntity.ok(addressService.deleteAddress(userId, addressId));
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<MessageResponse> setDefaultAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long addressId) {
        Long userId = extractUserId(jwt);
        return ResponseEntity.ok(addressService.setDefaultAddress(userId, addressId));
    }
}
