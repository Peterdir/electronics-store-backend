package com.ecommerce.backend.modules.review.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AdminReviewResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;
    private String productName;
    private String customerName;
    private Double rating;
    private String reviewText;
    private Instant createdAt;
    private boolean isHidden;
    private String adminReply;
}
