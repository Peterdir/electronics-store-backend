package com.ecommerce.backend.modules.dashboard.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.modules.auth.enums.Role;
import com.ecommerce.backend.modules.auth.repository.UserRepository;
import com.ecommerce.backend.modules.dashboard.dto.request.DashboardFilterRequest;
import com.ecommerce.backend.modules.dashboard.dto.response.DashboardOverviewResponse;
import com.ecommerce.backend.modules.dashboard.dto.response.DashboardStatsResponse;
import com.ecommerce.backend.modules.dashboard.dto.response.LowStockProductResponse;
import com.ecommerce.backend.modules.dashboard.dto.response.OrderStatusStatResponse;
import com.ecommerce.backend.modules.dashboard.dto.response.RevenueChartPointResponse;
import com.ecommerce.backend.modules.dashboard.mapper.DashboardMapper;
import com.ecommerce.backend.modules.inventory.entity.Inventory;
import com.ecommerce.backend.modules.inventory.repository.InventoryRepository;
import com.ecommerce.backend.modules.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;
    private final DashboardMapper dashboardMapper;

    private static final long LOW_STOCK_THRESHOLD = 10L;

    @Override
    @Transactional(readOnly = true)
    public DashboardOverviewResponse getDashboardOverview(DashboardFilterRequest request) {
        LocalDate endLocalDate = request != null && request.getEndDate() != null
                ? request.getEndDate()
                : LocalDate.now();

        LocalDate startLocalDate = request != null && request.getStartDate() != null
                ? request.getStartDate()
                : endLocalDate.minusDays(29);

        if (startLocalDate.isAfter(endLocalDate)) {
            throw new BadRequestException("Start date cannot be after end date.");
        }
        if (ChronoUnit.DAYS.between(startLocalDate, endLocalDate) > 365) {
            throw new BadRequestException("Date range cannot exceed 365 days.");
        }

        ZoneId zoneId = ZoneId.systemDefault();
        Instant startInstant = startLocalDate.atStartOfDay(zoneId).toInstant();
        Instant endInstant = endLocalDate.atTime(LocalTime.MAX).atZone(zoneId).toInstant();

        BigDecimal totalRevenue = orderRepository.calculateTotalRevenue(startInstant, endInstant);
        Long totalOrders = orderRepository.countOrdersBetween(startInstant, endInstant);
        Long newUsers = userRepository.countNewUsersBetween(Role.CUSTOMER, startInstant, endInstant);
        Long lowStockCount = inventoryRepository.countLowStockVariants(LOW_STOCK_THRESHOLD);

        DashboardStatsResponse stats = DashboardStatsResponse.builder()
                .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
                .totalOrders(totalOrders != null ? totalOrders : 0L)
                .newUsers(newUsers != null ? newUsers : 0L)
                .lowStockCount(lowStockCount != null ? lowStockCount : 0L)
                .build();

        List<Object[]> rawDailyData = orderRepository.getDailyRevenueAndOrders(startInstant, endInstant);
        Map<String, RevenueChartPointResponse> dateMap = new HashMap<>();

        for (Object[] row : rawDailyData) {
            RevenueChartPointResponse point = dashboardMapper.toRevenueChartPointResponse(row);
            if (point != null) {
                dateMap.put(point.getDate(), point);
            }
        }

        List<RevenueChartPointResponse> revenueChart = new ArrayList<>();
        LocalDate cur = startLocalDate;
        while (!cur.isAfter(endLocalDate)) {
            String dateKey = cur.toString();
            RevenueChartPointResponse point = dateMap.getOrDefault(
                    dateKey,
                    RevenueChartPointResponse.builder()
                            .date(dateKey)
                            .revenue(BigDecimal.ZERO)
                            .orderCount(0L)
                            .build()
            );
            revenueChart.add(point);
            cur = cur.plusDays(1);
        }

        List<OrderStatusStatResponse> orderStatusDistribution = orderRepository.countOrdersByStatus(startInstant, endInstant);

        List<Inventory> lowStockInventories = inventoryRepository.findTopLowStock(
                LOW_STOCK_THRESHOLD,
                PageRequest.of(0, 5)
        );

        List<LowStockProductResponse> lowStockAlerts = dashboardMapper.toLowStockProductResponses(lowStockInventories);

        return dashboardMapper.toOverviewResponse(stats, revenueChart, orderStatusDistribution, lowStockAlerts);
    }
}
