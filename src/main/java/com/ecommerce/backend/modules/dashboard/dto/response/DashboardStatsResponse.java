package com.ecommerce.backend.modules.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardStatsResponse {

    private BigDecimal totalRevenue;

    private Long totalOrders;

    private Long newUsers;

    private Long lowStockCount;
}
