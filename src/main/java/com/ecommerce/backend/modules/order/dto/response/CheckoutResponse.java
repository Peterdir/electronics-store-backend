package com.ecommerce.backend.modules.order.dto.response;
import com.ecommerce.backend.modules.order.enums.OrderStatus;
import com.ecommerce.backend.modules.order.enums.PaymentMethod;
import com.ecommerce.backend.modules.order.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckoutResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long orderId;

    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;

    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private String couponCode;
    private BigDecimal discountAmount;
    private BigDecimal totalPrice;

    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;

    private Instant createdAt;
    private List<OrderItemResponse> items;

    private String message;

    private String paymentUrl;

    private Boolean isGuestOrder;
}
