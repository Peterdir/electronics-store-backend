package com.ecommerce.backend.modules.review.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * DTO dùng cho tab chờ đánh giá
 */
@Data
@Builder
public class PendingReviewResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long orderItemId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long orderId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long productId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long productVariantId;

    private String productName;

    private Map<String, Object> variantAttributes;

    private String productImage;

    // Ngày đặt mua
    private Instant orderDate;
}
