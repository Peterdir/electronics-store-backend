package com.ecommerce.backend.modules.review.service;

import com.ecommerce.backend.modules.review.dto.request.ReviewRequest;
import com.ecommerce.backend.modules.review.dto.response.PendingReviewResponse;
import com.ecommerce.backend.modules.review.dto.response.ReviewResponse;

import java.util.List;

public interface ReviewService {

    List<ReviewResponse> getReviewedHistory(Long userId);

    List<PendingReviewResponse> getPendingReviews(Long userId);

    ReviewResponse createReview(Long userId, ReviewRequest request);

    ReviewResponse updateReview(Long userId, Long reviewId, ReviewRequest request);

    void deleteReview(Long userId, Long reviewId);
}
