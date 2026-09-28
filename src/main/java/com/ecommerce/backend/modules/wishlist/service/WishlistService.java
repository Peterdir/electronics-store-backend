package com.ecommerce.backend.modules.wishlist.service;

import com.ecommerce.backend.modules.auth.dto.response.MessageResponse;
import com.ecommerce.backend.modules.wishlist.dto.response.WishlistItemResponse;

import java.util.List;

public interface WishlistService {
    List<WishlistItemResponse> getWishlist(Long userId);
    MessageResponse addProductToWishlist(Long userId, Long productId);
    MessageResponse removeProductFromWishlist(Long userId, Long productId);
}
