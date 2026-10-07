package com.ecommerce.backend.modules.order.dto.response;

import com.ecommerce.backend.modules.order.enums.PaymentMethod;
import com.ecommerce.backend.modules.order.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaymentResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long orderId;

    private PaymentStatus paymentStatus;

    private PaymentMethod paymentMethod;

    private BigDecimal totalPrice;

    private String message;

    private String paymentUrl;
}
