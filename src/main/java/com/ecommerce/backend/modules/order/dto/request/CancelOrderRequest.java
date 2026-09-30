package com.ecommerce.backend.modules.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelOrderRequest {
    @NotBlank(message = "Cancellation reason is required")
    private String reason;
}
