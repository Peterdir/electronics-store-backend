package com.ecommerce.backend.modules.dashboard.dto.response;

import com.ecommerce.backend.modules.order.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderStatusStatResponse {

    private OrderStatus status;

    private Long count; // Số lượng đơn
}
