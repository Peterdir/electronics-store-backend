package com.ecommerce.backend.modules.product.dto.response;

import com.ecommerce.backend.modules.product.enums.ProductStatus;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VariantResponse {

    private Long id;

    private String sku;

    private BigDecimal price;

    private Long stock;

    private String imageUrl;

    private Map<String, Object> attributes;

    private ProductStatus status;
}
