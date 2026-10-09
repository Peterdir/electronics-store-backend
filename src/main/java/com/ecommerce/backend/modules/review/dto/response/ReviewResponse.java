package com.ecommerce.backend.modules.review.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class ReviewResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long productId;

    private String productName;

    private String productImage;

    private Map<String, Object> variantAttributes;

    private boolean isProductDeleted;

    private Double rating;

    private String reviewText;

    private String imageUrl;

    private Instant createdAt;
}
