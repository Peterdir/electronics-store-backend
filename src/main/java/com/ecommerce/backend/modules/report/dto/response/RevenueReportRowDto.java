package com.ecommerce.backend.modules.report.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueReportRowDto {
    private String date;
    private Long totalOrders;
    private BigDecimal grossRevenue;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private BigDecimal netRevenue;
}
