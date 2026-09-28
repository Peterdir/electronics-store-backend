package com.ecommerce.backend.modules.inventory.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;


import com.ecommerce.backend.modules.inventory.enums.InventoryStatus;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long productId;

    private String productName;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long variantId;

    private String sku;

    private Long quantity;

    private InventoryStatus status;

    private Instant updatedAt;
}
