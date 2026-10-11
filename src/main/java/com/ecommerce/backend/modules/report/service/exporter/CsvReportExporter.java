package com.ecommerce.backend.modules.report.service.exporter;

import com.ecommerce.backend.modules.report.dto.response.RevenueReportResponse;
import com.ecommerce.backend.modules.report.dto.response.RevenueReportRowDto;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

@Component
public class CsvReportExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] export(RevenueReportResponse report) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Write UTF-8 BOM for Microsoft Excel on Windows to properly display Vietnamese
        baos.write(0xEF);
        baos.write(0xBB);
        baos.write(0xBF);

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            // Title & Info
            writer.println(escape("BÁO CÁO DOANH THU BÁN HÀNG"));
            writer.println(escape(String.format("Khoảng thời gian: %s - %s",
                    report.getStartDate().format(DATE_FORMATTER),
                    report.getEndDate().format(DATE_FORMATTER))));
            writer.println(); // Empty line

            // Header
            writer.println(String.join(",",
                    escape("Ngày"),
                    escape("Số lượng đơn"),
                    escape("Doanh thu gốc"),
                    escape("Giảm giá / Coupon"),
                    escape("Phí vận chuyển"),
                    escape("Doanh thu thực nhận")
            ));

            // Data rows
            if (report.getItems() != null) {
                for (RevenueReportRowDto row : report.getItems()) {
                    long orders = row.getTotalOrders() != null ? row.getTotalOrders() : 0L;
                    BigDecimal gross = row.getGrossRevenue() != null ? row.getGrossRevenue() : BigDecimal.ZERO;
                    BigDecimal discount = row.getDiscountAmount() != null ? row.getDiscountAmount() : BigDecimal.ZERO;
                    BigDecimal shipping = row.getShippingFee() != null ? row.getShippingFee() : BigDecimal.ZERO;
                    BigDecimal net = row.getNetRevenue() != null ? row.getNetRevenue() : BigDecimal.ZERO;

                    writer.println(String.join(",",
                            escape(row.getDate()),
                            String.valueOf(orders),
                            gross.toPlainString(),
                            discount.toPlainString(),
                            shipping.toPlainString(),
                            net.toPlainString()
                    ));
                }
            }

            // Summary row - directly using calculated totals from RevenueReportResponse
            long totalOrders = report.getTotalOrders() != null ? report.getTotalOrders() : 0L;
            BigDecimal totalGross = report.getTotalGrossRevenue() != null ? report.getTotalGrossRevenue() : BigDecimal.ZERO;
            BigDecimal totalDiscount = report.getTotalDiscountAmount() != null ? report.getTotalDiscountAmount() : BigDecimal.ZERO;
            BigDecimal totalShipping = report.getTotalShippingFee() != null ? report.getTotalShippingFee() : BigDecimal.ZERO;
            BigDecimal totalNet = report.getTotalNetRevenue() != null ? report.getTotalNetRevenue() : BigDecimal.ZERO;

            writer.println(String.join(",",
                    escape("Tổng cộng"),
                    String.valueOf(totalOrders),
                    totalGross.toPlainString(),
                    totalDiscount.toPlainString(),
                    totalShipping.toPlainString(),
                    totalNet.toPlainString()
            ));

            writer.flush();
        }

        return baos.toByteArray();
    }

    private String escape(String value) {
        if (value == null) {
            return "\"\"";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
