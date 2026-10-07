package com.ecommerce.backend.modules.cart.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @Builder.Default
    private List<CartItemResponse> items = new ArrayList<>();

    private Integer totalItems;

    private BigDecimal totalPrice;
}
