package com.ecommerce.backend.modules.review.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.exception.DuplicateResourceException;
import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.modules.auth.entity.User;
import com.ecommerce.backend.modules.order.entity.Order;
import com.ecommerce.backend.modules.order.entity.OrderItem;
import com.ecommerce.backend.modules.order.enums.OrderStatus;
import com.ecommerce.backend.modules.order.repository.OrderItemRepository;
import com.ecommerce.backend.modules.product.entity.Product;
import com.ecommerce.backend.modules.product.entity.ProductImage;
import com.ecommerce.backend.modules.product.entity.ProductVariant;
import com.ecommerce.backend.modules.product.repository.ProductRepository;
import com.ecommerce.backend.common.service.FileStorageService;
import com.ecommerce.backend.common.utils.FileValidator;
import com.ecommerce.backend.modules.review.dto.request.ReviewReplyRequest;
import com.ecommerce.backend.modules.review.dto.request.ReviewRequest;
import com.ecommerce.backend.modules.review.dto.response.AdminReviewResponse;
import com.ecommerce.backend.modules.review.dto.response.PendingReviewResponse;
import com.ecommerce.backend.modules.review.dto.response.ReviewResponse;
import com.ecommerce.backend.modules.review.entity.Review;
import com.ecommerce.backend.modules.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final FileStorageService fileStorageService;
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
    public ReviewResponse createReview(Long userId, ReviewRequest request, MultipartFile image) {
        validateReviewContent(request.getReviewText());
        if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            throw new BadRequestException("Send review images as multipart files.");
        }

        OrderItem orderItem = orderItemRepository.findByIdForUpdate(request.getOrderItemId())
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem not found"));
        Order order = orderItem.getOrder();
        User user = order.getUser();

        if (user == null || !user.getId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to review this order item.");
        }
        if (order.getOrderStatus() != OrderStatus.DELIVERED) {
            throw new BadRequestException("Only delivered order items can be reviewed.");
        }

        ProductVariant variant = orderItem.getProductVariant();
        Product product = variant.getProduct();
        if (!product.getId().equals(request.getProductId())
                || (request.getProductVariantId() != null
                    && !variant.getId().equals(request.getProductVariantId()))) {
            throw new BadRequestException("Product or variant does not match the order item.");
        }
        if (reviewRepository.existsByUserIdAndOrderItemIdAndIsDeletedFalse(userId, orderItem.getId())) {
            throw new DuplicateResourceException("You have already reviewed this item.");
        }

        Review review = Review.builder()
                .user(user)
                .product(product)
                .productVariant(variant)
                .orderItem(orderItem)
                .rating(request.getRating())
                .reviewText(request.getReviewText())
                .imageUrl(image == null ? null : uploadReviewImage(image))
                .build();

        review = reviewRepository.save(review);
        updateProductAverageRating(product.getId());

        return mapToReviewResponse(review);
    }

    private String uploadReviewImage(MultipartFile image) {
        FileValidator.validateImageFile(image);
        String imageUrl = fileStorageService.uploadFile(image, "reviews");

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try {
                        fileStorageService.deleteFile(imageUrl);
                    } catch (RuntimeException ex) {
                        log.warn("Could not clean up review image after rollback: {}", imageUrl, ex);
                    }
                }
            }
        });

        return imageUrl;
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

    @Override
    @Transactional(readOnly = true)
    public Page<AdminReviewResponse> getReviewsForAdmin(Double rating, String productName, Pageable pageable) {
        Page<Review> reviewPage = reviewRepository.findAllForAdmin(rating, productName, pageable);

        return reviewPage.map(review -> AdminReviewResponse.builder()
                .id(review.getId())
                .productName(review.getProduct().getName())
                .customerName(review.getUser().getFullName())
                .rating(review.getRating())
                .reviewText(review.getReviewText())
                .createdAt(review.getCreatedAt())
                .isHidden(review.isHidden())
                .adminReply(review.getAdminReply())
                .build());
    }

    @Override
    @Transactional
    public void toggleReviewVisibility(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("This review has been deleted by the user and is no longer available."));

        if (review.isDeleted()) {
            throw new ResourceNotFoundException("This review has been deleted by the user and is no longer available.");
        }

        review.setHidden(!review.isHidden());
        reviewRepository.save(review);
    }

    @Override
    @Transactional
    public void replyToReview(Long reviewId, ReviewReplyRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("This review has been deleted by the user and is no longer available."));

        if (review.isDeleted()) {
            throw new ResourceNotFoundException("This review has been deleted by the user and is no longer available.");
        }

        review.setAdminReply(request.getReplyText());
        reviewRepository.save(review);
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
