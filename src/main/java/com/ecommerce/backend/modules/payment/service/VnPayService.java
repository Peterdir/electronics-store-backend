package com.ecommerce.backend.modules.payment.service;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface VnPayService {
    String createPaymentUrl(Long orderId, HttpServletRequest request);
    Map<String, Object> handleReturnUrl(HttpServletRequest request);
    Map<String, String> handleIpn(HttpServletRequest request);
}
