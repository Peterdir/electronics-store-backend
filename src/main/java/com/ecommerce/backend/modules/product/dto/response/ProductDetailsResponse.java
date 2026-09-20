package com.ecommerce.backend.modules.product.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDetailsResponse {

    private Long id;

    private String name;

    private BigDecimal basePrice;

    private String description;

    private Map<String, Object> specification;

    private List<String> images;

    private String categoryName;

    private String brandName;

    private List<VariantResponse> variants;

    private Long stock;
}
