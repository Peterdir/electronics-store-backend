package com.ecommerce.backend.modules.product.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;


import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCompareResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    private String name;

    private BigDecimal basePrice;

    private String imageUrl;

    private String brandName;

    private String categoryName;

    private Map<String, Object> specification;
}
