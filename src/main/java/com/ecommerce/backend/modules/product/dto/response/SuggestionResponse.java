package com.ecommerce.backend.modules.product.dto.response;


import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuggestionResponse {

    private Long id;

    private String name;

    private String imageUrl;

    private BigDecimal basePrice;

}
