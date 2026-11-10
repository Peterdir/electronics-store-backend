package com.ecommerce.backend.modules.product.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.ecommerce.backend.modules.product.enums.ProductStatus;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariantResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    private String sku;

    private BigDecimal price;

    private Long stock;

    private String imageUrl;

    private List<ProductImageDetailResponse> images;

    private Map<String, Object> attributes;

    private ProductStatus status;
}
