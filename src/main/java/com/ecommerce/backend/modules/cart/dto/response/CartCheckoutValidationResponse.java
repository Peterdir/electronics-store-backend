package com.ecommerce.backend.modules.cart.dto.response;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartCheckoutValidationResponse {

    private Boolean isValid;

    private String message;

    @Builder.Default
    private List<CartItemIssueResponse> issues = new ArrayList<>();
}
