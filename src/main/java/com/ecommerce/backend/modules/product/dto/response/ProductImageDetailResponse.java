package com.ecommerce.backend.modules.product.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImageDetailResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    private String imageUrl;

    private int displayOrder;

    private Boolean isPrimary;
}
