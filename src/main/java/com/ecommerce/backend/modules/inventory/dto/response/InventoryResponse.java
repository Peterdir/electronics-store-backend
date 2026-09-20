package com.ecommerce.backend.modules.inventory.dto.response;


import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryResponse {

    private Long id;

    private Long productId;

    private String productName;

    private Long variantId;

    private String sku;

    private Long quantity;

    private String status;

    private Instant updatedAt;
}
