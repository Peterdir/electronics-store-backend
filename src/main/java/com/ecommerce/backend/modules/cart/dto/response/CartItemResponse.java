package com.ecommerce.backend.modules.cart.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponse {

    private Long id;

    private Long productVariantId;

    private Long productId;

    private String productName;

    private String sku;

    private String imageUrl;

    private Map<String, Object> attributes;

    private BigDecimal unitPrice;

    private Integer quantity;

    private BigDecimal subtotal;

    private Long availableStock;

    private Boolean isOutOfStock;

    private Boolean hasSufficientStock;
}
