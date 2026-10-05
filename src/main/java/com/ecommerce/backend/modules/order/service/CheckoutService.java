package com.ecommerce.backend.modules.order.service;

import com.ecommerce.backend.modules.order.dto.request.CheckoutRequest;
import com.ecommerce.backend.modules.order.dto.response.CheckoutResponse;

public interface CheckoutService {

    /**
     * Đặt hàng dành cho Registered User (Có userId từ JWT)
     * Hỗ trợ Address Book
     * */
    CheckoutResponse placeOrder(Long userId, CheckoutRequest request);

    /**
     * Đặt hàng dành cho Guest (Không có JWT)
     * Bắt buộc nhập shipping address
     * */
    CheckoutResponse placeGuestOrder(CheckoutRequest request);
}
