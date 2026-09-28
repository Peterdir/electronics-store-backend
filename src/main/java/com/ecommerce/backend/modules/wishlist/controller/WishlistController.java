package com.ecommerce.backend.modules.wishlist.controller;

import com.ecommerce.backend.modules.auth.dto.response.MessageResponse;
import com.ecommerce.backend.modules.wishlist.dto.response.WishlistItemResponse;
import com.ecommerce.backend.modules.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/wishlists")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    private Long extractUserId(Jwt jwt) {
        return Long.valueOf(Objects.requireNonNull(jwt.getSubject(), "JWT subject must not be null"));
    }

    @GetMapping
    public ResponseEntity<List<WishlistItemResponse>> getWishlist(@AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserId(jwt);
        return ResponseEntity.ok(wishlistService.getWishlist(userId));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<MessageResponse> addProductToWishlist(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long productId) {
        Long userId = extractUserId(jwt);
        return ResponseEntity.ok(wishlistService.addProductToWishlist(userId, productId));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<MessageResponse> removeProductFromWishlist(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long productId) {
        Long userId = extractUserId(jwt);
        return ResponseEntity.ok(wishlistService.removeProductFromWishlist(userId, productId));
    }
}
