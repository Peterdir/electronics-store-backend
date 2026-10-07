package com.ecommerce.backend.modules.order.dto.response;

import com.ecommerce.backend.modules.order.enums.OrderStatus;
import com.ecommerce.backend.modules.order.enums.PaymentMethod;
import com.ecommerce.backend.modules.order.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Builder(toBuilder = true)
public class OrderDetailAdminResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    // Thông tin khách hàng
    private Long userId;
    private String customerName;
    private String customerEmail;
    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;

    // Thông tin đơn hàng
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private String couponCode;
    private BigDecimal discountAmount;
    private BigDecimal totalPrice;

    // Trạng thái đơn hàng
    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private String cancelReason;
    private Instant cancelledAt;

    private Instant createdAt;
    private Instant updatedAt;

    private String message;

    // Danh sách sản phẩm
    private List<OrderItemResponse> items;
}
