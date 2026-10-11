package com.ecommerce.backend.modules.report.mapper;

import com.ecommerce.backend.modules.report.dto.response.ExportedReportFile;
import com.ecommerce.backend.modules.report.dto.response.RevenueReportResponse;
import com.ecommerce.backend.modules.report.dto.response.RevenueReportRowDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Component
public class ReportMapper {

    public RevenueReportRowDto toRevenueReportRow(Object[] row) {
        if (row == null || row.length < 6) {
            return null;
        }

        String reportDate = String.valueOf(row[0]);
        Long totalOrders = row[1] != null ? ((Number) row[1]).longValue() : 0L;
        BigDecimal grossRevenue = row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO;
        BigDecimal discountAmount = row[3] != null ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO;
        BigDecimal shippingFee = row[4] != null ? new BigDecimal(row[4].toString()) : BigDecimal.ZERO;
        BigDecimal netRevenue = row[5] != null ? new BigDecimal(row[5].toString()) : BigDecimal.ZERO;

        return RevenueReportRowDto.builder()
                .date(reportDate)
                .totalOrders(totalOrders)
                .grossRevenue(grossRevenue)
                .discountAmount(discountAmount)
                .shippingFee(shippingFee)
                .netRevenue(netRevenue)
                .build();
    }

    public RevenueReportRowDto createEmptyRevenueReportRow(String date) {
        return RevenueReportRowDto.builder()
                .date(date)
                .totalOrders(0L)
                .grossRevenue(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .shippingFee(BigDecimal.ZERO)
                .netRevenue(BigDecimal.ZERO)
                .build();
    }

    public List<RevenueReportRowDto> toContinuousRevenueReportRows(List<Object[]> rawData, LocalDate startDate, LocalDate endDate) {
        Map<String, RevenueReportRowDto> dateMap = new HashMap<>();

        if (rawData != null) {
            for (Object[] row : rawData) {
                RevenueReportRowDto rowDto = toRevenueReportRow(row);
                if (rowDto != null) {
                    dateMap.put(rowDto.getDate(), rowDto);
                }
            }
        }

        List<RevenueReportRowDto> result = new ArrayList<>();
        LocalDate cur = startDate;
        while (!cur.isAfter(endDate)) {
            String dateKey = cur.toString();
            RevenueReportRowDto row = dateMap.getOrDefault(dateKey, createEmptyRevenueReportRow(dateKey));
            result.add(row);
            cur = cur.plusDays(1);
        }

        return result;
    }

    public RevenueReportResponse toRevenueReportResponse(LocalDate startDate, LocalDate endDate, List<RevenueReportRowDto> items) {
        long totalOrders = 0;
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        BigDecimal totalShipping = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;

        if (items != null) {
            for (RevenueReportRowDto item : items) {
                totalOrders += item.getTotalOrders() != null ? item.getTotalOrders() : 0L;
                totalGross = totalGross.add(item.getGrossRevenue() != null ? item.getGrossRevenue() : BigDecimal.ZERO);
                totalDiscount = totalDiscount.add(item.getDiscountAmount() != null ? item.getDiscountAmount() : BigDecimal.ZERO);
                totalShipping = totalShipping.add(item.getShippingFee() != null ? item.getShippingFee() : BigDecimal.ZERO);
                totalNet = totalNet.add(item.getNetRevenue() != null ? item.getNetRevenue() : BigDecimal.ZERO);
            }
        }

        return RevenueReportResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalOrders(totalOrders)
                .totalGrossRevenue(totalGross)
                .totalDiscountAmount(totalDiscount)
                .totalShippingFee(totalShipping)
                .totalNetRevenue(totalNet)
                .items(items != null ? items : Collections.emptyList())
                .build();
    }

    public ExportedReportFile toExportedFile(String filename, String contentType, byte[] content) {
        return ExportedReportFile.builder()
                .filename(filename)
                .contentType(contentType)
                .content(content)
                .build();
    }
}
