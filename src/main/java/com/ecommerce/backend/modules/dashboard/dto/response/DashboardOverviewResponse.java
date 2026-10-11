package com.ecommerce.backend.modules.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardOverviewResponse {

    private DashboardStatsResponse stats;

    private List<RevenueChartPointResponse> revenueChart;

    private List<OrderStatusStatResponse> orderStatusDistribution;

    private List<LowStockProductResponse> lowStockAlerts;
}
