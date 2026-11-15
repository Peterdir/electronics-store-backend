package com.ecommerce.backend.modules.review.controller;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.modules.review.dto.request.ReviewRequest;
import com.ecommerce.backend.modules.review.dto.request.UpdateReviewRequest;
import com.ecommerce.backend.modules.review.dto.response.PendingReviewResponse;
import com.ecommerce.backend.modules.review.dto.response.ReviewResponse;
import com.ecommerce.backend.modules.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReviewResponse> createReview(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ReviewRequest request) {

        return createdReview(jwt, request, null);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReviewResponse> createReviewWithImage(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @ModelAttribute ReviewRequest request,
            @RequestParam(name = "image", required = false) List<MultipartFile> images) {

        return createdReview(jwt, request, singleReviewImage(images));
    }

    private MultipartFile singleReviewImage(List<MultipartFile> images) {
        if (images != null && images.size() > 1) {
            throw new BadRequestException("Only one review image is allowed.");
        }

        return images == null || images.isEmpty() ? null : images.getFirst();
    }

    private ResponseEntity<ReviewResponse> createdReview(Jwt jwt, ReviewRequest request, MultipartFile image) {
        ReviewResponse response = reviewService.createReview(extractUserId(jwt), request, image);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping(value = "/{reviewId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReviewResponse> updateReview(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateReviewRequest request) {

        Long userId = extractUserId(jwt);
        return ResponseEntity.ok(reviewService.updateReview(userId, reviewId, request, null));
    }

    @PutMapping(value = "/{reviewId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReviewResponse> updateReviewWithImage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long reviewId,
            @Valid @ModelAttribute UpdateReviewRequest request,
            @RequestParam(name = "image", required = false) List<MultipartFile> images) {

        Long userId = extractUserId(jwt);
        return ResponseEntity.ok(reviewService.updateReview(userId, reviewId, request, singleReviewImage(images)));
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
