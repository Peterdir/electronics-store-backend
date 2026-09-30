package com.ecommerce.backend.modules.order.dto.request;

import com.ecommerce.backend.modules.order.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrderStatusRequest {
    @NotNull(message = "Order status is required")
    private OrderStatus newStatus;
}
