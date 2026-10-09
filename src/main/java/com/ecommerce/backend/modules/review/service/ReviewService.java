package com.ecommerce.backend.modules.review.service;

import com.ecommerce.backend.modules.review.dto.request.ReviewReplyRequest;
import com.ecommerce.backend.modules.review.dto.request.ReviewRequest;
import com.ecommerce.backend.modules.review.dto.response.AdminReviewResponse;
import com.ecommerce.backend.modules.review.dto.response.PendingReviewResponse;
import com.ecommerce.backend.modules.review.dto.response.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ReviewService {

    List<ReviewResponse> getReviewedHistory(Long userId);

    List<PendingReviewResponse> getPendingReviews(Long userId);

    ReviewResponse createReview(Long userId, ReviewRequest request);

    ReviewResponse updateReview(Long userId, Long reviewId, ReviewRequest request);

    void deleteReview(Long userId, Long reviewId);

    Page<AdminReviewResponse> getReviewsForAdmin(Double rating, String productName, Pageable pageable);

    void toggleReviewVisibility(Long reviewId);

    void replyToReview(Long reviewId, ReviewReplyRequest request);
}
