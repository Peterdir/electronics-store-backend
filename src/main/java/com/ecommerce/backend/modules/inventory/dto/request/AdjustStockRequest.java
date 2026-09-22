package com.ecommerce.backend.modules.inventory.dto.request;

import com.ecommerce.backend.modules.inventory.enums.InventoryAction;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdjustStockRequest {

    @NotNull(message = "Quantity must be a valid number greater than 0.")
    @Min(value = 1, message = "Quantity must be a valid number greater than 0.")
    private Long quantity;

    private InventoryAction action;

    private String reason;
}
