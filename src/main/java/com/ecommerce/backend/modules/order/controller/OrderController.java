package com.ecommerce.backend.modules.order.controller;

import com.ecommerce.backend.modules.order.dto.request.CancelOrderRequest;
import com.ecommerce.backend.modules.order.dto.request.CheckoutRequest;
import com.ecommerce.backend.modules.order.dto.response.*;
import com.ecommerce.backend.modules.order.enums.OrderStatus;
import com.ecommerce.backend.modules.order.service.CheckoutService;
import com.ecommerce.backend.modules.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CheckoutService checkoutService;

    private Long extractUserId(Jwt jwt) {
        return Long.valueOf(Objects.requireNonNull(jwt.getSubject(), "JWT subject must not be null"));
    }

    @GetMapping
    public ResponseEntity<Page<OrderListUserResponse>> getMyOrders(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) OrderStatus orderStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Long userId = extractUserId(jwt);
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(orderService.getMyOrders(userId, orderStatus, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailUserResponse> getMyOrderDetail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        Long userId = extractUserId(jwt);
        return ResponseEntity.ok(orderService.getMyOrderDetail(userId, id));
    }

    @PostMapping("/{id}/buy-again")
    public ResponseEntity<BuyAgainResponse> buyAgain(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        Long userId = extractUserId(jwt);
        return ResponseEntity.ok(orderService.buyAgain(userId, id));
    }

    @RequestMapping(value = "/{id}/cancel", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<OrderDetailUserResponse> cancelMyOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody CancelOrderRequest request
    ) {
        Long userId = extractUserId(jwt);
        return ResponseEntity.ok(orderService.cancelMyOrder(userId, id, request));
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<OrderPaymentResponse> payOrder(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        Long userId = extractUserId(jwt);
        return ResponseEntity.ok(orderService.payOrder(userId, id));
    }

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutResponse> checkout(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CheckoutRequest request
            ) {
        if (jwt != null) {
            Long userId = extractUserId(jwt);
            return ResponseEntity.ok(checkoutService.placeOrder(userId, request));
        }
        else {
            return ResponseEntity.ok(checkoutService.placeGuestOrder(request));
        }
    }
}
