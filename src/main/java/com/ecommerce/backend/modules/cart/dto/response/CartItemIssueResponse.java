package com.ecommerce.backend.modules.cart.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemIssueResponse {

    private Long cartItemId;

    private Long productVariantId;

    private String productName;

    private Integer requestedQuantity;

    private Long availableStock;

    private String message;
}
