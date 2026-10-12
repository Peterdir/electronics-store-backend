package com.ecommerce.backend.modules.product.dto.response;

import com.ecommerce.backend.modules.product.enums.ProductStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminProductListResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    private String name;

    private BigDecimal basePrice;

    private String primaryImageUrl;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long categoryId;

    private String categoryName;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long brandId;

    private String brandName;

    private ProductStatus status;

    private Long totalStock;

    private Integer variantCount;

    private Instant createdAt;

    private Instant updatedAt;
}
