package com.ecommerce.backend.modules.inventory.dto.response;

import com.ecommerce.backend.modules.inventory.enums.InventoryAction;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryHistoryResponse {

    private Long id;

    private InventoryAction action;

    private Long quantityChanged;

    private Long finalStock;

    private String reason;

    private String performedBy;

    private Instant createdAt;
}
