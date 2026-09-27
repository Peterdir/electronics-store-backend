package com.ecommerce.backend.modules.cart.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.modules.auth.dto.response.MessageResponse;
import com.ecommerce.backend.modules.auth.entity.User;
import com.ecommerce.backend.modules.auth.repository.UserRepository;
import com.ecommerce.backend.modules.cart.dto.request.AddToCartRequest;
import com.ecommerce.backend.modules.cart.dto.request.UpdateCartItemRequest;
import com.ecommerce.backend.modules.cart.dto.response.CartCheckoutValidationResponse;
import com.ecommerce.backend.modules.cart.dto.response.CartItemIssueResponse;
import com.ecommerce.backend.modules.cart.dto.response.CartItemResponse;
import com.ecommerce.backend.modules.cart.dto.response.CartResponse;
import com.ecommerce.backend.modules.cart.entity.Cart;
import com.ecommerce.backend.modules.cart.entity.CartItem;
import com.ecommerce.backend.modules.cart.repository.CartItemRepository;
import com.ecommerce.backend.modules.cart.repository.CartRepository;
import com.ecommerce.backend.modules.product.entity.ProductVariant;
import com.ecommerce.backend.modules.product.enums.ProductStatus;
import com.ecommerce.backend.modules.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService{

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    public CartResponse getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return mapToCartResponse(cart);
    }

    @Override
    public CartResponse addCart(Long userId, AddToCartRequest request) {
        Cart cart = getOrCreateCart(userId);

        ProductVariant variant = productVariantRepository.findById(request.getProductVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Product variant not found."));

        if (variant.getProduct().getStatus() != ProductStatus.ACTIVE || variant.getStatus() != ProductStatus.ACTIVE) {
            throw new BadRequestException("This product is currently not available for purchase.");
        }

        Long stock = getAvailableStock(variant);

        Optional<CartItem> existingItemOpt = cart.getItems().stream()
                .filter(item -> item.getProductVariant().getId().equals(variant.getId()))
                .findFirst();

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + request.getQuantity();

            if (newQuantity > stock) {
                throw new BadRequestException("Only " + stock + " items left in stock.");
            }

            existingItem.setQuantity(newQuantity);
        } else {
            if (request.getQuantity() > stock) {
                throw new BadRequestException("Only " + stock + " items left in stock.");
            }

            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .productVariant(variant)
                    .quantity(request.getQuantity())
                    .build();
            cart.getItems().add(newItem);
        }

        Cart savedCart = cartRepository.save(cart);
        return mapToCartResponse(savedCart);
    }

    @Override
    public CartResponse updateItemQuantity(Long userId, Long cartItemId, UpdateCartItemRequest request) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found."));

        if (!cartItem.getCart().getUser().getId().equals(userId)) {
            throw new BadRequestException("You don't have permission.");
        }

        ProductVariant variant = cartItem.getProductVariant();
        Long stock = getAvailableStock(variant);

        if (request.getQuantity() > stock) {
            throw new BadRequestException("Only " + stock + " items left in stock.");
        }
        cartItem.setQuantity(request.getQuantity());
        cartItemRepository.save(cartItem);

        return mapToCartResponse(cartItem.getCart());
    }

    @Override
    public CartResponse removeItem(Long userId, Long cartItemId) {
        CartItem cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found."));

        if (!cartItem.getCart().getUser().getId().equals(userId)) {
            throw new BadRequestException("You don't have permission.");
        }

        Cart cart = cartItem.getCart();
        // Do cơ chế của orphanRemoval nên khi xóa cartItem ở cart thì dữ liệu ở bảng cartItem cũng tự động được xóa
        cart.getItems().remove(cartItem);
        cartRepository.save(cart);

        return mapToCartResponse(cart);
    }

    @Override
    public MessageResponse clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);

        cart.getItems().clear();
        cartRepository.save(cart);

        return MessageResponse.builder()
                .message("Cart cleared successfully.")
                .build();
    }

    @Override
    public CartCheckoutValidationResponse validateCartForCheckout(Long userId) {
        Cart cart = getOrCreateCart(userId);

        if (cart.getItems().isEmpty()) {
            return CartCheckoutValidationResponse.builder()
                    .isValid(false)
                    .message("Your cart is currently empty.")
                    .issues(new ArrayList<>())
                    .build();
        }

        // Mảng lưu trữ các vấn đề
        List<CartItemIssueResponse> issues = new ArrayList<>();

        for (CartItem item : cart.getItems()) {
            ProductVariant variant = item.getProductVariant();
            Long stock = getAvailableStock(variant);
            boolean isInactive = variant.getProduct().getStatus() != ProductStatus.ACTIVE
                    || variant.getStatus() != ProductStatus.ACTIVE;

            if (isInactive) {
                issues.add(CartItemIssueResponse.builder()
                        .cartItemId(item.getId())
                        .productVariantId(variant.getId())
                        .productName(variant.getProduct().getName())
                        .requestedQuantity(item.getQuantity())
                        .availableStock(stock)
                        .message("This item is no longer available.")
                        .build());
            } else if (stock <= 0) {
                issues.add(CartItemIssueResponse.builder()
                        .cartItemId(item.getId())
                        .productVariantId(variant.getId())
                        .productName(variant.getProduct().getName())
                        .requestedQuantity(item.getQuantity())
                        .availableStock(0L)
                        .message("This item is out of stock.")
                        .build());
            } else if (item.getQuantity() > stock) {
                issues.add(CartItemIssueResponse.builder()
                        .cartItemId(item.getId())
                        .productVariantId(variant.getId())
                        .productName(variant.getProduct().getName())
                        .requestedQuantity(item.getQuantity())
                        .availableStock(stock)
                        .message("Only " + stock + " items left in stock.")
                        .build());
            }
        }

        if (!issues.isEmpty()) {
            return CartCheckoutValidationResponse.builder()
                    .isValid(false)
                    .message("Some items in your cart are no longer available in the requested quantity. Please update your cart to proceed.")
                    .issues(issues)
                    .build();
        }

        return CartCheckoutValidationResponse.builder()
                .isValid(true)
                .message("Cart is valid for checkout.")
                .issues(new ArrayList<>())
                .build();
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("Not found user."));
                    Cart newCart = Cart.builder()
                            .user(user)
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    private Long getAvailableStock(ProductVariant variant) {
        if (variant.getInventory() != null && variant.getInventory().getQuantity() != null) {
            return variant.getInventory().getQuantity();
        }

        return 0L;
    }

    private CartResponse mapToCartResponse(Cart cart) {
        List<CartItemResponse> itemResponses = new ArrayList<>();

        int totalItems = 0;
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (CartItem item : cart.getItems()) {
            ProductVariant variant = item.getProductVariant();

            BigDecimal  unitPrice = variant.getPrice() != null ? variant.getPrice() : BigDecimal.ZERO;

            int quantity = item.getQuantity() != null ? item.getQuantity() : 0;

            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

            Long stock = getAvailableStock(variant);

            boolean isOutOfStock = stock <= 0;
            boolean hasSufficientStock = stock >= quantity;

            CartItemResponse itemResponse = CartItemResponse.builder()
                    .id(item.getId())
                    .productVariantId(variant.getId())
                    .productId(variant.getProduct() != null ? variant.getProduct().getId() : null)
                    .productName(variant.getProduct() != null ? variant.getProduct().getName() : "")
                    .sku(variant.getSku())
                    .imageUrl(variant.getImageUrl())
                    .attributes(variant.getAttributes())
                    .unitPrice(unitPrice)
                    .quantity(quantity)
                    .subtotal(subtotal)
                    .availableStock(stock)
                    .isOutOfStock(isOutOfStock)
                    .hasSufficientStock(hasSufficientStock)
                    .build();

            itemResponses.add(itemResponse);

            totalItems += quantity;
            totalPrice = totalPrice.add(subtotal);
        }

        return CartResponse.builder()
                .id(cart.getId())
                .items(itemResponses)
                .totalItems(totalItems)
                .totalPrice(totalPrice)
                .build();
    }
}
