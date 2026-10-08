package com.ecommerce.backend.modules.review.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ReviewResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long productId;

    private String productName;

    private String productImage;

    private String variantName;

    private boolean isProductDeleted;

    private Double rating;

    private String reviewText;

    private String imageUrl;

    private Instant createdAt;
}
