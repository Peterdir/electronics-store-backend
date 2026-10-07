package com.ecommerce.backend.modules.order.dto.response;

import com.ecommerce.backend.modules.cart.dto.response.CartResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuyAgainResponse {

    private String message;

    private int addedItemsCount;

    private int unavailableItemsCount;

    private List<String> addedItemNames;

    private List<String> unavailableItemNames;

    private CartResponse cart;
}
