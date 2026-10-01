package com.ecommerce.backend.modules.order.dto.response;

import com.ecommerce.backend.modules.order.enums.OrderStatus;
import com.ecommerce.backend.modules.order.enums.PaymentMethod;
import com.ecommerce.backend.modules.order.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailUserResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    // Thông tin người nhận
    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;

    // Chi tiết tài chính
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private String couponCode;
    private BigDecimal discountAmount;
    private BigDecimal totalPrice;

    // Trạng thái đơn hàng
    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;

    // Thông tin hủy đơn
    private String cancelReason;
    private Instant cancelledAt;

    // Thời gian
    private Instant createdAt;
    private Instant updatedAt;

    // Cờ hành động
    private Boolean canCancel;
    private Boolean canPay;
    private Boolean canBuyAgain;

    // Danh sách sản phẩm
    private List<OrderItemResponse> items;
}
