package com.ecommerce.backend.modules.order.dto.response;

import com.ecommerce.backend.modules.order.enums.OrderStatus;
import com.ecommerce.backend.modules.order.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class OrderListAdminResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    private String customerName;

    private String customerEmail;

    private BigDecimal totalPrice;

    private Instant orderDate;

    private PaymentStatus paymentStatus;

    private OrderStatus orderStatus;
}
