package com.ecommerce.backend.modules.cart.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {

    private Long id;

    @Builder.Default
    private List<CartItemResponse> items = new ArrayList<>();

    private Integer totalItems;

    private BigDecimal totalPrice;
}
