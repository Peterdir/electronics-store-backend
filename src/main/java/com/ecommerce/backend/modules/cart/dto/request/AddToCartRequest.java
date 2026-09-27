package com.ecommerce.backend.modules.cart.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddToCartRequest {

    @NotNull(message = "Product variant ID is required.")
    private Long productVariantId;

    @NotNull(message = "Please enter a valid quantity.")
    @Min(value = 1, message = "Please enter a valid quantity.")
    private Integer quantity;
}
