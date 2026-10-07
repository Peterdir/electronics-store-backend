package com.ecommerce.backend.modules.product.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;


import lombok.*;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuggestionResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    private String name;

    private String imageUrl;

    private BigDecimal basePrice;

}
