package com.ecommerce.backend.modules.dashboard.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LowStockProductResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long productVariantId;

    private String productName;

    private String sku;

    private Long quantity;
}
