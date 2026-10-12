package com.ecommerce.backend.modules.review.controller;

import com.ecommerce.backend.modules.auth.dto.response.MessageResponse;
import com.ecommerce.backend.modules.review.dto.request.ReviewReplyRequest;
import com.ecommerce.backend.modules.review.dto.response.AdminReviewResponse;
import com.ecommerce.backend.modules.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
public class AdminReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<Page<AdminReviewResponse>> getReviews(
            @RequestParam(required = false) Double rating,
            @RequestParam(required = false) String productName,
            Pageable pageable) {
        return ResponseEntity.ok(reviewService.getReviewsForAdmin(rating, productName, pageable));
    }

    @PatchMapping("/{id}/toggle-visibility")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<MessageResponse> toggleVisibility(@PathVariable Long id) {
        reviewService.toggleReviewVisibility(id);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Review visibility updated successfully.")
                .build());
    }

    @PostMapping("/{id}/reply")
    public ResponseEntity<MessageResponse> replyReview(
            @PathVariable Long id, 
            @Valid @RequestBody ReviewReplyRequest request) {
        reviewService.replyToReview(id, request);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Reply posted successfully.")
                .build());
    }
}
