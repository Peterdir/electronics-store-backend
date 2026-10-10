package com.ecommerce.backend.modules.payment.controller;

import com.ecommerce.backend.modules.payment.service.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final VnPayService vnPayService;

    @GetMapping("/vnpay/create")
    public RedirectView createPayment(@RequestParam Long orderId, HttpServletRequest request) {
        String paymentUrl = vnPayService.createPaymentUrl(orderId, request);

        return new RedirectView(paymentUrl);
    }

    @GetMapping("/vnpay/return")
    public ResponseEntity<Map<String, Object>> vnpayReturn(HttpServletRequest request) {
        Map<String, Object> result = vnPayService.handleReturnUrl(request);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/vnpay/ipn")
    public ResponseEntity<Map<String, String>> vnpayIpn(HttpServletRequest request) {
        Map<String, String> result = vnPayService.handleIpn(request);

        return ResponseEntity.ok(result);
    }

}
