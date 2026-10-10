package com.ecommerce.backend.modules.review.controller;

import com.ecommerce.backend.modules.auth.entity.User;
import com.ecommerce.backend.modules.review.dto.request.ReviewRequest;
import com.ecommerce.backend.modules.review.dto.response.PendingReviewResponse;
import com.ecommerce.backend.modules.review.dto.response.ReviewResponse;
import com.ecommerce.backend.modules.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    private Long extractUserId(Jwt jwt) {
        return Long.valueOf(Objects.requireNonNull(jwt.getSubject(), "JWT subject must not be null"));
    }

    @GetMapping("/me/history")
    public ResponseEntity<List<ReviewResponse>> getMyReviews(@AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserId(jwt);
        return ResponseEntity.ok(reviewService.getReviewedHistory(userId));
    }

    @GetMapping("/me/pending")
    public ResponseEntity<List<PendingReviewResponse>> getPendingReviews(@AuthenticationPrincipal Jwt jwt) {
        Long userId = extractUserId(jwt);
        return ResponseEntity.ok(reviewService.getPendingReviews(userId));
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> createReview(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ReviewRequest request) {

        Long userId = extractUserId(jwt);
        ReviewResponse response = reviewService.createReview(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ReviewResponse> updateReview(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewRequest request) {

        Long userId = extractUserId(jwt);
        return ResponseEntity.ok(reviewService.updateReview(userId, reviewId, request));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long reviewId) {

        Long userId = extractUserId(jwt);
        reviewService.deleteReview(userId, reviewId);
        return ResponseEntity.noContent().build();
    }

}
