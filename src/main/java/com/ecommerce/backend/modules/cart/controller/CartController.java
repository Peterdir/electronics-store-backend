package com.ecommerce.backend.modules.cart.controller;

import com.ecommerce.backend.modules.auth.dto.response.MessageResponse;
import com.ecommerce.backend.modules.cart.dto.request.AddToCartRequest;
import com.ecommerce.backend.modules.cart.dto.request.UpdateCartItemRequest;
import com.ecommerce.backend.modules.cart.dto.response.CartCheckoutValidationResponse;
import com.ecommerce.backend.modules.cart.dto.response.CartResponse;
import com.ecommerce.backend.modules.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@Controller
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    private Long extractUserId(Jwt jwt) {
        return Long.valueOf(Objects.requireNonNull(jwt.getSubject(), "JWT subject must not be null"));
    }

    @GetMapping
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserId(jwt);
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addCart(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddToCartRequest request) {
        Long userId = extractUserId(jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(cartService.addCart(userId, request));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<CartResponse> updateItemQuantity(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long cartItemId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        Long userId = extractUserId(jwt);
        return ResponseEntity.ok(cartService.updateItemQuantity(userId, cartItemId, request));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<CartResponse> removeItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long cartItemId) {
        Long userId = extractUserId(jwt);
        return ResponseEntity.ok(cartService.removeItem(userId, cartItemId));
    }

    @DeleteMapping
    public ResponseEntity<MessageResponse> clearCart(@AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserId(jwt);
        return ResponseEntity.ok(cartService.clearCart(userId));
    }

    @PostMapping("/validate-checkout")
    public ResponseEntity<CartCheckoutValidationResponse> validateCartForCheckout(@AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserId(jwt);
        return ResponseEntity.ok(cartService.validateCartForCheckout(userId));
    }
}
