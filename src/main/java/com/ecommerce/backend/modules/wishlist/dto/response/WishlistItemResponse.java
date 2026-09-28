package com.ecommerce.backend.modules.wishlist.dto.response;

import com.ecommerce.backend.modules.product.enums.ProductStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WishlistItemResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id; // Wishlist entry id
    
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long productId;
    private String productName;
    private BigDecimal price;
    private String primaryImageUrl;
    private boolean inStock;
    private ProductStatus productStatus;
    private Instant addedAt;
}
