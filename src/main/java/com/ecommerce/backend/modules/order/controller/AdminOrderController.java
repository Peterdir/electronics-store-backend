package com.ecommerce.backend.modules.order.controller;

import com.ecommerce.backend.modules.order.dto.request.CancelOrderRequest;
import com.ecommerce.backend.modules.order.dto.request.UpdateOrderStatusRequest;
import com.ecommerce.backend.modules.order.dto.response.OrderDetailAdminResponse;
import com.ecommerce.backend.modules.order.dto.response.OrderListAdminResponse;
import com.ecommerce.backend.modules.order.enums.OrderStatus;
import com.ecommerce.backend.modules.order.enums.PaymentStatus;
import com.ecommerce.backend.modules.order.service.OrderAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final OrderAdminService orderAdminService;

    @GetMapping
    public ResponseEntity<Page<OrderListAdminResponse>> getOrders(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false) OrderStatus orderStatus,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(orderAdminService.getOrders(keyword, orderStatus, paymentStatus, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailAdminResponse> getOrderDetail(@PathVariable Long id) {
        return ResponseEntity.ok(orderAdminService.getOrderDetail(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderDetailAdminResponse> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request
    ) {
        return ResponseEntity.ok(orderAdminService.updateOrderStatus(id, request));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<OrderDetailAdminResponse> cancelOrder(
            @PathVariable Long id,
            @Valid @RequestBody CancelOrderRequest request
    ) {
        return ResponseEntity.ok(orderAdminService.cancelOrder(id, request));
    }
}
