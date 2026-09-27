package com.ecommerce.backend.modules.cart.service;

import com.ecommerce.backend.modules.auth.dto.response.MessageResponse;
import com.ecommerce.backend.modules.cart.dto.request.AddToCartRequest;
import com.ecommerce.backend.modules.cart.dto.request.UpdateCartItemRequest;
import com.ecommerce.backend.modules.cart.dto.response.CartCheckoutValidationResponse;
import com.ecommerce.backend.modules.cart.dto.response.CartResponse;

public interface CartService {

    CartResponse getCart(Long userId);

    CartResponse addCart(Long userId, AddToCartRequest request);

    CartResponse updateItemQuantity(Long userId, Long cartItemId, UpdateCartItemRequest request);

    CartResponse removeItem(Long userId, Long cartItemId);

    MessageResponse clearCart(Long userId);

    CartCheckoutValidationResponse validateCartForCheckout(Long userId);
}
