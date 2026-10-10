package com.ecommerce.backend.modules.coupon.controller;

import com.ecommerce.backend.modules.coupon.dto.request.CouponRequest;
import com.ecommerce.backend.modules.coupon.dto.response.CouponResponse;
import com.ecommerce.backend.modules.coupon.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCouponController {

    private final CouponService couponService;

    @GetMapping
    public ResponseEntity<List<CouponResponse>> getAllCoupons() {
        return ResponseEntity.ok(couponService.getAllCoupons());
    }

    @PostMapping
    public ResponseEntity<CouponResponse> createCoupon(@Valid @RequestBody CouponRequest request) {
        CouponResponse createdCoupon = couponService.createCoupon(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdCoupon);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CouponResponse> updateCoupon(
            @PathVariable Long id, 
            @Valid @RequestBody CouponRequest request) {
        CouponResponse updatedCoupon = couponService.updateCoupon(id, request);
        return ResponseEntity.ok(updatedCoupon);
    }

    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<String> toggleCouponStatus(@PathVariable Long id) {
        couponService.toggleCouponStatus(id);
        return ResponseEntity.ok("Coupon status updated successfully.");
    }
}
