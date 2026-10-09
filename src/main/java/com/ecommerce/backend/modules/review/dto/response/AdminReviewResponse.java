package com.ecommerce.backend.modules.review.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AdminReviewResponse {
    private Long id;
    private String productName;
    private String customerName;
    private Double rating;
    private String reviewText;
    private Instant createdAt;
    private boolean isHidden;
    private String adminReply;
}
