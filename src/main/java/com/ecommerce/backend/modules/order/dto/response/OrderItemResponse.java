package com.ecommerce.backend.modules.order.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderItemResponse {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long variantId;

    private String productName;

    private String variantSku;

    private String thumbnailUrl;

    private BigDecimal unitPrice;

    private Integer quantity;

    private BigDecimal subtotal;
}
