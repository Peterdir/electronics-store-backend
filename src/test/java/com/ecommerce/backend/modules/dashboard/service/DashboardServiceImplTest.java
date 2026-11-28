package com.ecommerce.backend.modules.dashboard.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.modules.auth.enums.Role;
import com.ecommerce.backend.modules.auth.repository.UserRepository;
import com.ecommerce.backend.modules.dashboard.dto.request.DashboardFilterRequest;
import com.ecommerce.backend.modules.dashboard.dto.response.DashboardOverviewResponse;
import com.ecommerce.backend.modules.dashboard.dto.response.OrderStatusStatResponse;
import com.ecommerce.backend.modules.dashboard.mapper.DashboardMapper;
import com.ecommerce.backend.modules.inventory.entity.Inventory;
import com.ecommerce.backend.modules.inventory.repository.InventoryRepository;
import com.ecommerce.backend.modules.order.enums.OrderStatus;
import com.ecommerce.backend.modules.order.repository.OrderRepository;
import com.ecommerce.backend.modules.product.entity.Product;
import com.ecommerce.backend.modules.product.entity.ProductVariant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardServiceImpl Unit Test Suite")
class DashboardServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Spy
    private DashboardMapper dashboardMapper = new DashboardMapper();

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    @Nested
    @DisplayName("Get Dashboard Overview Tests")
    class GetDashboardOverviewTests {

        @Test
        @DisplayName("Should default to last 30 days when filter request is null and default null stats to zero")
        void getDashboardOverview_NullRequest_UsesDefault30DaysAndZeroDefaults() {
            when(orderRepository.calculateTotalRevenue(any(Instant.class), any(Instant.class))).thenReturn(null);
            when(orderRepository.countOrdersBetween(any(Instant.class), any(Instant.class))).thenReturn(null);
            when(userRepository.countNewUsersBetween(eq(Role.CUSTOMER), any(Instant.class), any(Instant.class)))
                    .thenReturn(null);
            when(inventoryRepository.countLowStockVariants(10L)).thenReturn(null);
            when(orderRepository.getDailyRevenueAndOrders(any(Instant.class), any(Instant.class)))
                    .thenReturn(Collections.emptyList());
            when(orderRepository.countOrdersByStatus(any(Instant.class), any(Instant.class)))
                    .thenReturn(Collections.emptyList());
            when(inventoryRepository.findTopLowStock(10L, PageRequest.of(0, 5)))
                    .thenReturn(Collections.emptyList());

            DashboardOverviewResponse response = dashboardService.getDashboardOverview(null);

            assertNotNull(response);
            assertEquals(BigDecimal.ZERO, response.getStats().getTotalRevenue());
            assertEquals(0L, response.getStats().getTotalOrders());
            assertEquals(0L, response.getStats().getNewUsers());
            assertEquals(0L, response.getStats().getLowStockCount());
            assertEquals(30, response.getRevenueChart().size());
            assertEquals(LocalDate.now().minusDays(29).toString(), response.getRevenueChart().get(0).getDate());
            assertEquals(LocalDate.now().toString(), response.getRevenueChart().get(29).getDate());

            verify(userRepository).countNewUsersBetween(eq(Role.CUSTOMER), any(Instant.class), any(Instant.class));
            verify(inventoryRepository).findTopLowStock(10L, PageRequest.of(0, 5));
        }

        @Test
        @DisplayName("Should fill sparse daily revenue gaps, map status distribution, and map low stock alerts")
        void getDashboardOverview_ExplicitRange_FillsGapsAndMapsAllSections() {
            LocalDate start = LocalDate.of(2026, 11, 1);
            LocalDate end = LocalDate.of(2026, 11, 5);
            DashboardFilterRequest request = new DashboardFilterRequest();
            request.setStartDate(start);
            request.setEndDate(end);

            when(orderRepository.calculateTotalRevenue(any(), any())).thenReturn(new BigDecimal("1500000"));
            when(orderRepository.countOrdersBetween(any(), any())).thenReturn(4L);
            when(userRepository.countNewUsersBetween(eq(Role.CUSTOMER), any(), any())).thenReturn(3L);
            when(inventoryRepository.countLowStockVariants(10L)).thenReturn(2L);

            // Chỉ có dữ liệu ngày 01 và 04; kèm 1 row thiếu cột để kiểm tra an toàn
            List<Object[]> rawRows = List.of(
                    new Object[]{"2026-11-01", new BigDecimal("1000000"), 3L},
                    new Object[]{"2026-11-04", new BigDecimal("500000"), 1L},
                    new Object[]{"invalid"}
            );
            when(orderRepository.getDailyRevenueAndOrders(any(), any())).thenReturn(rawRows);

            List<OrderStatusStatResponse> statusStats = List.of(
                    OrderStatusStatResponse.builder().status(OrderStatus.DELIVERED).count(3L).build(),
                    OrderStatusStatResponse.builder().status(OrderStatus.PENDING).count(1L).build()
            );
            when(orderRepository.countOrdersByStatus(any(), any())).thenReturn(statusStats);

            Product product = Product.builder().id(10L).name("MacBook Pro").build();
            ProductVariant variant = ProductVariant.builder().id(100L).sku("MBP-14").product(product).build();
            Inventory inv1 = Inventory.builder().id(1L).productVariant(variant).quantity(3L).build();
            Inventory invNullVariant = Inventory.builder().id(2L).productVariant(null).quantity(1L).build();
            when(inventoryRepository.findTopLowStock(10L, PageRequest.of(0, 5)))
                    .thenReturn(List.of(inv1, invNullVariant));

            DashboardOverviewResponse response = dashboardService.getDashboardOverview(request);

            assertEquals(new BigDecimal("1500000"), response.getStats().getTotalRevenue());
            assertEquals(4L, response.getStats().getTotalOrders());
            assertEquals(3L, response.getStats().getNewUsers());
            assertEquals(2L, response.getStats().getLowStockCount());

            assertEquals(5, response.getRevenueChart().size());
            assertEquals(new BigDecimal("1000000"), response.getRevenueChart().get(0).getRevenue());
            assertEquals(3L, response.getRevenueChart().get(0).getOrderCount());
            assertEquals(BigDecimal.ZERO, response.getRevenueChart().get(1).getRevenue()); // 2026-11-02
            assertEquals(0L, response.getRevenueChart().get(1).getOrderCount());
            assertEquals(new BigDecimal("500000"), response.getRevenueChart().get(3).getRevenue()); // 2026-11-04

            assertEquals(2, response.getOrderStatusDistribution().size());
            assertEquals(2, response.getLowStockAlerts().size());
            assertEquals("MacBook Pro", response.getLowStockAlerts().get(0).getProductName());
            assertEquals("MBP-14", response.getLowStockAlerts().get(0).getSku());
            assertEquals(3L, response.getLowStockAlerts().get(0).getQuantity());
            assertNull(response.getLowStockAlerts().get(1).getProductVariantId());
        }

        @Test
        @DisplayName("Should allow single-day and exact 365-day boundary ranges")
        void getDashboardOverview_BoundaryDateRanges_Succeed() {
            when(orderRepository.getDailyRevenueAndOrders(any(), any())).thenReturn(Collections.emptyList());
            when(orderRepository.countOrdersByStatus(any(), any())).thenReturn(Collections.emptyList());
            when(inventoryRepository.findTopLowStock(eq(10L), any())).thenReturn(Collections.emptyList());

            DashboardFilterRequest singleDay = new DashboardFilterRequest();
            singleDay.setStartDate(LocalDate.of(2026, 11, 20));
            singleDay.setEndDate(LocalDate.of(2026, 11, 20));
            assertEquals(1, dashboardService.getDashboardOverview(singleDay).getRevenueChart().size());

            DashboardFilterRequest maxRange = new DashboardFilterRequest();
            LocalDate end = LocalDate.of(2026, 11, 27);
            maxRange.setStartDate(end.minusDays(365));
            maxRange.setEndDate(end);
            assertEquals(366, dashboardService.getDashboardOverview(maxRange).getRevenueChart().size());
        }

        @Test
        @DisplayName("Should throw BadRequestException when startDate > endDate or range exceeds 365 days")
        void getDashboardOverview_InvalidDateRanges_ThrowBadRequest() {
            DashboardFilterRequest reversed = new DashboardFilterRequest();
            reversed.setStartDate(LocalDate.of(2026, 11, 10));
            reversed.setEndDate(LocalDate.of(2026, 11, 5));
            BadRequestException ex1 = assertThrows(BadRequestException.class,
                    () -> dashboardService.getDashboardOverview(reversed));
            assertTrue(ex1.getMessage().contains("Start date cannot be after end date"));

            DashboardFilterRequest tooLong = new DashboardFilterRequest();
            tooLong.setEndDate(LocalDate.of(2026, 11, 27));
            tooLong.setStartDate(LocalDate.of(2026, 11, 27).minusDays(366));
            BadRequestException ex2 = assertThrows(BadRequestException.class,
                    () -> dashboardService.getDashboardOverview(tooLong));
            assertTrue(ex2.getMessage().contains("cannot exceed 365 days"));

            verifyNoInteractions(orderRepository, userRepository, inventoryRepository);
        }
    }
}
