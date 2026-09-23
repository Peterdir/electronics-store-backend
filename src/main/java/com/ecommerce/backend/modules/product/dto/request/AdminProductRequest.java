package com.ecommerce.backend.modules.product.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminProductRequest {

    @NotBlank(message = "This field is required.")
    private String name;

    @NotNull(message = "This field is required.")
    private Long categoryId;

    private Long brandId;

    @NotNull(message = "This field is required.")
    @Min(value = 0, message = "Value must be a valid number greater than or equal to 0.")
    private BigDecimal basePrice;

    private String description;

    private Map<String, Object> specification;

    @Valid
    private List<AdminProductVariantRequest> variants;
}
