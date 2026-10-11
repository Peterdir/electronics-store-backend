package com.ecommerce.backend.modules.report.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueReportResponse {
    private LocalDate startDate;
    private LocalDate endDate;
    private Long totalOrders;
    private BigDecimal totalGrossRevenue;
    private BigDecimal totalDiscountAmount;
    private BigDecimal totalShippingFee;
    private BigDecimal totalNetRevenue;
    private List<RevenueReportRowDto> items;
}
