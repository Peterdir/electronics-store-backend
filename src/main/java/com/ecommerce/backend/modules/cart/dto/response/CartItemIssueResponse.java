package com.ecommerce.backend.modules.cart.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemIssueResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long cartItemId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long productVariantId;

    private String productName;

    private Integer requestedQuantity;

    private Long availableStock;

    private String message;
}
