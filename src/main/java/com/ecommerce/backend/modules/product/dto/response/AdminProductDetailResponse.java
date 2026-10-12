package com.ecommerce.backend.modules.product.dto.response;

import com.ecommerce.backend.modules.product.enums.ProductStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminProductDetailResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    private String name;

    private BigDecimal basePrice;

    private String description;

    private Map<String, Object> specification;

    private ProductStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long categoryId;

    private String categoryName;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long brandId;

    private String brandName;

    private List<ProductImageDetailResponse> images;

    private List<VariantResponse> variants;

    private Long totalStock;

    private Instant createdAt;

    private Instant updatedAt;
}
