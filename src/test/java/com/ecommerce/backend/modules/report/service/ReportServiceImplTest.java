package com.ecommerce.backend.modules.report.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.modules.order.repository.OrderRepository;
import com.ecommerce.backend.modules.report.dto.request.ReportFilterRequest;
import com.ecommerce.backend.modules.report.dto.response.ExportedReportFile;
import com.ecommerce.backend.modules.report.dto.response.RevenueReportResponse;
import com.ecommerce.backend.modules.report.mapper.ReportMapper;
import com.ecommerce.backend.modules.report.service.exporter.CsvReportExporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportServiceImpl Unit Test Suite")
class ReportServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Spy
    private ReportMapper reportMapper = new ReportMapper();

    @Spy
    private CsvReportExporter csvReportExporter = new CsvReportExporter();

    @InjectMocks
    private ReportServiceImpl reportService;

    private ReportFilterRequest range(LocalDate start, LocalDate end) {
        return ReportFilterRequest.builder()
                .startDate(start)
                .endDate(end)
                .build();
    }

    @Nested
    @DisplayName("Request Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should reject null request, missing dates, reversed dates, future endDate, and >365-day range")
        void validateRequest_InvalidInputs_ThrowBadRequest() {
            LocalDate today = LocalDate.now();

            assertThrows(BadRequestException.class, () -> reportService.previewReport(null));
            assertThrows(BadRequestException.class, () -> reportService.previewReport(range(null, today)));
            assertThrows(BadRequestException.class, () -> reportService.previewReport(range(today, null)));
            assertThrows(BadRequestException.class,
                    () -> reportService.previewReport(range(today, today.minusDays(1))));
            assertThrows(BadRequestException.class,
                    () -> reportService.previewReport(range(today.minusDays(5), today.plusDays(1))));
            assertThrows(BadRequestException.class,
                    () -> reportService.exportReport(range(today.minusDays(366), today)));

            verifyNoInteractions(orderRepository);
        }

        @Test
        @DisplayName("Should accept boundary 365-day range ending today")
        void validateRequest_Exact365DaysEndingToday_Succeeds() {
            LocalDate today = LocalDate.now();
            when(orderRepository.getDailyRevenueReportData(any(Instant.class), any(Instant.class)))
                    .thenReturn(Collections.emptyList());

            RevenueReportResponse response = reportService.previewReport(range(today.minusDays(365), today));

            assertEquals(366, response.getItems().size());
            assertEquals(0L, response.getTotalOrders());
        }
    }

    @Nested
    @DisplayName("Preview Report Tests")
    class PreviewReportTests {

        @Test
        @DisplayName("Should return continuous zero-filled daily rows and accurate aggregated totals")
        void previewReport_SparseData_FillsMissingDaysAndAggregatesTotals() {
            LocalDate start = LocalDate.of(2026, 11, 1);
            LocalDate end = LocalDate.of(2026, 11, 3);

            List<Object[]> rawRows = List.of(
                    new Object[]{"2026-11-01", 2L, new BigDecimal("500000"), new BigDecimal("50000"),
                            new BigDecimal("30000"), new BigDecimal("480000")},
                    new Object[]{"2026-11-03", null, null, null, null, new BigDecimal("100000")},
                    new Object[]{"short"}
            );
            when(orderRepository.getDailyRevenueReportData(any(Instant.class), any(Instant.class)))
                    .thenReturn(rawRows);

            RevenueReportResponse response = reportService.previewReport(range(start, end));

            assertNotNull(response);
            assertEquals(3, response.getItems().size());
            assertEquals(2L, response.getTotalOrders());
            assertEquals(new BigDecimal("500000"), response.getTotalGrossRevenue());
            assertEquals(new BigDecimal("50000"), response.getTotalDiscountAmount());
            assertEquals(new BigDecimal("30000"), response.getTotalShippingFee());
            assertEquals(new BigDecimal("580000"), response.getTotalNetRevenue());

            // Ngày 2026-11-02 được điền 0
            assertEquals("2026-11-02", response.getItems().get(1).getDate());
            assertEquals(0L, response.getItems().get(1).getTotalOrders());
            assertEquals(BigDecimal.ZERO, response.getItems().get(1).getNetRevenue());
        }
    }

    @Nested
    @DisplayName("Export CSV Report Tests")
    class ExportReportTests {

        @Test
        @DisplayName("Should export UTF-8 BOM CSV file with Vietnamese headers, daily rows, and summary row")
        void exportReport_WithData_ReturnsFormattedCsvFile() {
            LocalDate start = LocalDate.of(2026, 11, 1);
            LocalDate end = LocalDate.of(2026, 11, 2);

            List<Object[]> rawRows = List.<Object[]>of(
                    new Object[]{"2026-11-01", 3L, new BigDecimal("900000"), new BigDecimal("100000"),
                            new BigDecimal("30000"), new BigDecimal("830000")}
            );
            when(orderRepository.getDailyRevenueReportData(any(Instant.class), any(Instant.class)))
                    .thenReturn(rawRows);

            ExportedReportFile file = reportService.exportReport(range(start, end));

            assertEquals("revenue_report_20261101_20261102.csv", file.getFilename());
            assertEquals("text/csv; charset=UTF-8", file.getContentType());

            byte[] bytes = file.getContent();
            assertThat(bytes.length).isGreaterThan(3);
            // UTF-8 BOM cho Excel trên Windows
            assertEquals((byte) 0xEF, bytes[0]);
            assertEquals((byte) 0xBB, bytes[1]);
            assertEquals((byte) 0xBF, bytes[2]);

            String csvText = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
            assertThat(csvText).contains("BÁO CÁO DOANH THU BÁN HÀNG");
            assertThat(csvText).contains("01/11/2026 - 02/11/2026");
            assertThat(csvText).contains("\"2026-11-01\",3,900000,100000,30000,830000");
            assertThat(csvText).contains("\"2026-11-02\",0,0,0,0,0");
            assertThat(csvText).contains("\"Tổng cộng\",3,900000,100000,30000,830000");
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when no report data exists for export")
        void exportReport_EmptyRawData_ThrowsResourceNotFound() throws IOException {
            LocalDate start = LocalDate.of(2026, 11, 1);
            LocalDate end = LocalDate.of(2026, 11, 2);
            when(orderRepository.getDailyRevenueReportData(any(Instant.class), any(Instant.class)))
                    .thenReturn(Collections.emptyList());

            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> reportService.exportReport(range(start, end)));

            assertThat(ex.getMessage()).contains("No data found for the selected period");
            verify(csvReportExporter, never()).export(any());
        }

        @Test
        @DisplayName("Should wrap IOException from CsvReportExporter into BadRequestException")
        void exportReport_ExporterThrowsIOException_ThrowsBadRequest() throws IOException {
            LocalDate start = LocalDate.of(2026, 11, 1);
            LocalDate end = LocalDate.of(2026, 11, 1);

            when(orderRepository.getDailyRevenueReportData(any(Instant.class), any(Instant.class)))
                    .thenReturn(List.<Object[]>of(
                            new Object[]{"2026-11-01", 1L, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.TEN}
                    ));
            doThrow(new IOException("disk full")).when(csvReportExporter).export(any());

            assertThrows(BadRequestException.class, () -> reportService.exportReport(range(start, end)));
        }
    }
}
