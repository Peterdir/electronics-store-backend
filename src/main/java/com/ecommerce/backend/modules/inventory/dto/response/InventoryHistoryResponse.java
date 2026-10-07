package com.ecommerce.backend.modules.inventory.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.ecommerce.backend.modules.inventory.enums.InventoryAction;
import lombok.*;

import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryHistoryResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long id;

    private InventoryAction action;

    private Long quantityChanged;

    private Long finalStock;

    private String reason;

    private String performedBy;

    private Instant createdAt;
}
