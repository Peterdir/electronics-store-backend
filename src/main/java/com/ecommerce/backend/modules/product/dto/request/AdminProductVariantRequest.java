package com.ecommerce.backend.modules.product.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminProductVariantRequest {

    private String sku;

    @NotNull(message = "This field is required.")
    @Min(value = 0, message = "Value must be a valid number greater than or equal to 0.")
    private BigDecimal price;

    @NotNull(message = "This field is required.")
    @Min(value = 0, message = "Value must be a valid number greater than or equal to 0.")
    private Long stock;

    private Map<String, Object> attributes;
}
