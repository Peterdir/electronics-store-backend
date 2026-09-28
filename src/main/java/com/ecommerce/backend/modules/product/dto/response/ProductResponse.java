package com.ecommerce.backend.modules.product.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.ecommerce.backend.modules.product.enums.ProductStatus;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    private String name;

    private BigDecimal basePrice;

    private String primaryImageUrl;

    private String categoryName;

    private String brandName;

    private ProductStatus status;

}
