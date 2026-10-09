package com.ecommerce.backend.modules.review.controller;

import com.ecommerce.backend.modules.review.dto.request.ReviewReplyRequest;
import com.ecommerce.backend.modules.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<?> getReviews(
            @RequestParam(required = false) Double rating,
            @RequestParam(required = false) String productName,
            Pageable pageable) {
        return ResponseEntity.ok(reviewService.getReviewsForAdmin(rating, productName, pageable));
    }

    @PatchMapping("/{id}/toggle-visibility")
    public ResponseEntity<?> toggleVisibility(@PathVariable Long id) {
        reviewService.toggleReviewVisibility(id);
        return ResponseEntity.ok(Map.of("message", "Review visibility updated successfully."));
    }

    @PostMapping("/{id}/reply")
    public ResponseEntity<?> replyReview(
            @PathVariable Long id, 
            @Valid @RequestBody ReviewReplyRequest request) {
        reviewService.replyToReview(id, request);
        return ResponseEntity.ok(Map.of("message", "Reply posted successfully."));
    }
}
