package com.ecommerce.backend.modules.order.dto.response;

import com.ecommerce.backend.modules.order.enums.OrderStatus;
import com.ecommerce.backend.modules.order.enums.PaymentMethod;
import com.ecommerce.backend.modules.order.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderListUserResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    private BigDecimal totalPrice;

    private Instant orderDate;

    private OrderStatus orderStatus;

    private PaymentStatus paymentStatus;

    private PaymentMethod paymentMethod;

    private Integer totalItems;

    private Boolean canCancel;

    private Boolean canPay;

    private Boolean canBuyAgain;

    private List<OrderItemResponse> items;
}
