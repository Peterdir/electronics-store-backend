package com.ecommerce.backend.modules.dashboard.mapper;

import com.ecommerce.backend.modules.dashboard.dto.response.DashboardOverviewResponse;
import com.ecommerce.backend.modules.dashboard.dto.response.DashboardStatsResponse;
import com.ecommerce.backend.modules.dashboard.dto.response.LowStockProductResponse;
import com.ecommerce.backend.modules.dashboard.dto.response.OrderStatusStatResponse;
import com.ecommerce.backend.modules.dashboard.dto.response.RevenueChartPointResponse;
import com.ecommerce.backend.modules.inventory.entity.Inventory;
import com.ecommerce.backend.modules.product.entity.Product;
import com.ecommerce.backend.modules.product.entity.ProductVariant;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Component
public class DashboardMapper {

    public LowStockProductResponse toLowStockProductResponse(Inventory inventory) {
        if (inventory == null) {
            return null;
        }

        ProductVariant variant = inventory.getProductVariant();
        Product product = variant != null ? variant.getProduct() : null;

        return LowStockProductResponse.builder()
                .productVariantId(variant != null ? variant.getId() : null)
                .productName(product != null ? product.getName() : null)
                .sku(variant != null ? variant.getSku() : null)
                .quantity(inventory.getQuantity())
                .build();
    }

    public List<LowStockProductResponse> toLowStockProductResponses(List<Inventory> inventories) {
        if (inventories == null) {
            return Collections.emptyList();
        }
        return inventories.stream()
                .map(this::toLowStockProductResponse)
                .toList();
    }

    public RevenueChartPointResponse toRevenueChartPointResponse(Object[] row) {
        if (row == null || row.length < 3) {
            return null;
        }

        String dateStr = String.valueOf(row[0]);
        BigDecimal rev = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
        Long orders = row[2] != null ? ((Number) row[2]).longValue() : 0L;

        return RevenueChartPointResponse.builder()
                .date(dateStr)
                .revenue(rev)
                .orderCount(orders)
                .build();
    }

    public DashboardOverviewResponse toOverviewResponse(
            DashboardStatsResponse stats,
            List<RevenueChartPointResponse> revenueChart,
            List<OrderStatusStatResponse> orderStatusDistribution,
            List<LowStockProductResponse> lowStockAlerts
    ) {
        return DashboardOverviewResponse.builder()
                .stats(stats)
                .revenueChart(revenueChart)
                .orderStatusDistribution(orderStatusDistribution)
                .lowStockAlerts(lowStockAlerts)
                .build();
    }
}
