package com.ecommerce.backend.modules.review.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.exception.DuplicateResourceException;
import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.modules.auth.entity.User;
import com.ecommerce.backend.modules.auth.repository.UserRepository;
import com.ecommerce.backend.modules.order.entity.OrderItem;
import com.ecommerce.backend.modules.order.enums.OrderStatus;
import com.ecommerce.backend.modules.order.repository.OrderItemRepository;
import com.ecommerce.backend.modules.product.entity.Product;
import com.ecommerce.backend.modules.product.entity.ProductImage;
import com.ecommerce.backend.modules.product.entity.ProductVariant;
import com.ecommerce.backend.modules.product.repository.ProductRepository;
import com.ecommerce.backend.modules.product.repository.ProductVariantRepository;
import com.ecommerce.backend.modules.review.dto.request.ReviewRequest;
import com.ecommerce.backend.modules.review.dto.response.PendingReviewResponse;
import com.ecommerce.backend.modules.review.dto.response.ReviewResponse;
import com.ecommerce.backend.modules.review.entity.Review;
import com.ecommerce.backend.modules.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final OrderItemRepository orderItemRepository;

    private static final List<String> BLACKLIST_WORDS = Arrays.asList("chửi_thề", "cấm", "http://", "https://");

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewedHistory(Long userId) {
        List<Review> reviews = reviewRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);

        return reviews.stream()
                .map(this::mapToReviewResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingReviewResponse> getPendingReviews(Long userId) {
        List<OrderItem> pendingItems = orderItemRepository.findPendingReviewItems(userId, OrderStatus.DELIVERED);

        return pendingItems.stream()
                .map(item -> PendingReviewResponse.builder()
                        .orderItemId(item.getId())
                        .orderId(item.getOrder().getId())
                        .productId(item.getProductVariant().getProduct().getId())
                        .productVariantId(item.getProductVariant().getId())
                        .productName(item.getProductVariant().getProduct().getName())
                        .variantAttributes(item.getProductVariant().getAttributes())
                        .orderDate(item.getOrder().getCreatedAt())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public ReviewResponse createReview(Long userId, ReviewRequest request) {
        validateReviewContent(request.getReviewText());

        if (reviewRepository.existsByUserIdAndOrderItemIdAndIsDeletedFalse(userId, request.getOrderItemId())) {
            throw new DuplicateResourceException("You have already reviewed this item.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        OrderItem orderItem = orderItemRepository.findById(request.getOrderItemId())
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem not found"));

        ProductVariant variant = request.getProductVariantId() != null
                ? productVariantRepository.findById(request.getProductVariantId()).orElse(null)
                : null;

        Review review = Review.builder()
                .user(user)
                .product(product)
                .productVariant(variant)
                .orderItem(orderItem)
                .rating(request.getRating())
                .reviewText(request.getReviewText())
                .imageUrl(request.getImageUrl())
                .build();

        review = reviewRepository.save(review);
        updateProductAverageRating(product.getId());

        return mapToReviewResponse(review);
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(Long userId, Long reviewId, ReviewRequest request) {
        validateReviewContent(request.getReviewText());

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (!review.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Unauthorized action");
        }

        review.setRating(request.getRating());
        review.setReviewText(request.getReviewText());
        review.setImageUrl(request.getImageUrl());

        reviewRepository.save(review);
        updateProductAverageRating(review.getProduct().getId());

        return mapToReviewResponse(review);
    }

    @Override
    @Transactional
    public void deleteReview(Long userId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (!review.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Unauthorized action");
        }

        review.setDeleted(true);
        reviewRepository.save(review);

        updateProductAverageRating(review.getProduct().getId());
    }

    private void validateReviewContent(String text) {
        if (text == null || text.isBlank())
            return;

        String lowerText = text.toLowerCase();
        for (String word : BLACKLIST_WORDS) {
            if (lowerText.contains(word)) {
                throw new BadRequestException("Your review contains inappropriate content or links. Please modify it.");
            }
        }
    }

    private void updateProductAverageRating(Long productId) {
        Double avg = reviewRepository.getAverageRatingByProductId(productId);
        if (avg == null)
            avg = 0.0;

        Product product = productRepository.findById(productId).orElse(null);
        if (product != null) {
            product.setAverageRating(avg);
            productRepository.save(product);
        }
    }

    private ReviewResponse mapToReviewResponse(Review review) {
        Product product = review.getProduct();
        boolean isDeleted = (product == null);

        String mainImageUrl = null;

        if (product != null && product.getProductImages() != null) {
            mainImageUrl = product.getProductImages().stream()
                    .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                    .map(ProductImage::getImageUrl)
                    .findFirst()
                    .orElse(null);
        }

        return ReviewResponse.builder()
                .id(review.getId())
                .reviewText(review.getReviewText())
                .imageUrl(review.getImageUrl())
                .rating(review.getRating())
                .createdAt(review.getCreatedAt())
                .isProductDeleted(isDeleted)
                .productId(isDeleted ? null : product.getId())
                .productName(isDeleted ? "Product no longer available" : product.getName())
                .productImage(isDeleted ? null : mainImageUrl)
                .variantAttributes(
                        review.getProductVariant() != null ? review.getProductVariant().getAttributes() : null)
                .build();
    }
}
