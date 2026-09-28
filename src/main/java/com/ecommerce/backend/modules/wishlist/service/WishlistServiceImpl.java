package com.ecommerce.backend.modules.wishlist.service;

import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.modules.auth.dto.response.MessageResponse;
import com.ecommerce.backend.modules.auth.entity.User;
import com.ecommerce.backend.modules.auth.repository.UserRepository;
import com.ecommerce.backend.modules.product.entity.Product;
import com.ecommerce.backend.modules.product.entity.ProductImage;
import com.ecommerce.backend.modules.product.repository.ProductRepository;
import com.ecommerce.backend.modules.wishlist.dto.response.WishlistItemResponse;
import com.ecommerce.backend.modules.wishlist.entity.Wishlist;
import com.ecommerce.backend.modules.wishlist.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Override
    public List<WishlistItemResponse> getWishlist(Long userId) {
        List<Wishlist> wishlists = wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return wishlists.stream().map(this::mapToResponse).toList();
    }

    @Override
    public MessageResponse addProductToWishlist(Long userId, Long productId) {
        if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            return MessageResponse.builder().message("Product is already in your wishlist.").build();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found."));

        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .product(product)
                .build();
        wishlistRepository.save(wishlist);

        return MessageResponse.builder().message("Product added to your wishlist.").build();
    }

    @Override
    public MessageResponse removeProductFromWishlist(Long userId, Long productId) {
        if (!wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new ResourceNotFoundException("Product not found in wishlist.");
        }

        wishlistRepository.deleteByUserIdAndProductId(userId, productId);

        return MessageResponse.builder()
                .message("Product removed from your wishlist.")
                .build();
    }

    private WishlistItemResponse mapToResponse(Wishlist wishlist) {
        Product product = wishlist.getProduct();
        
        String primaryImageUrl = null;
        if (product.getProductImages() != null && !product.getProductImages().isEmpty()) {
            primaryImageUrl = product.getProductImages().stream()
                    .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                    .map(ProductImage::getImageUrl)
                    .findFirst()
                    .orElseGet(() -> product.getProductImages().get(0).getImageUrl());
        }

        boolean inStock = false;
        if (product.getVariants() != null) {
            long totalStock = product.getVariants().stream()
                .filter(v -> v.getInventory() != null && v.getInventory().getQuantity() != null)
                .mapToLong(v -> v.getInventory().getQuantity())
                .sum();
            inStock = totalStock > 0;
        }

        return WishlistItemResponse.builder()
                .id(wishlist.getId())
                .productId(product.getId())
                .productName(product.getName())
                .price(product.getBasePrice())
                .primaryImageUrl(primaryImageUrl)
                .inStock(inStock)
                .productStatus(product.getStatus())
                .addedAt(wishlist.getCreatedAt())
                .build();
    }
}
