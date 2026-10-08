package com.ecommerce.backend.modules.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewRequest {

    @NotNull(message = "Please select a star rating.")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Double rating;

    private String reviewText;

    private String imageUrl;

    @NotNull(message = "Product ID is required")
    private Long productId;

    private Long productVariantId;

    @NotNull(message = "Order Item ID is required")
    private Long orderItemId;
}
