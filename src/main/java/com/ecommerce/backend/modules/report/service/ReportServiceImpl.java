package com.ecommerce.backend.modules.report.service;

import com.ecommerce.backend.common.exception.BadRequestException;
import com.ecommerce.backend.common.exception.ResourceNotFoundException;
import com.ecommerce.backend.modules.order.repository.OrderRepository;
import com.ecommerce.backend.modules.report.dto.request.ReportFilterRequest;
import com.ecommerce.backend.modules.report.dto.response.ExportedReportFile;
import com.ecommerce.backend.modules.report.dto.response.RevenueReportResponse;
import com.ecommerce.backend.modules.report.dto.response.RevenueReportRowDto;
import com.ecommerce.backend.modules.report.mapper.ReportMapper;
import com.ecommerce.backend.modules.report.service.exporter.CsvReportExporter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final OrderRepository orderRepository;
    private final ReportMapper reportMapper;
    private final CsvReportExporter csvReportExporter;

    private static final String CSV_CONTENT_TYPE = "text/csv; charset=UTF-8";

    @Override
    @Transactional(readOnly = true)
    public RevenueReportResponse previewReport(ReportFilterRequest request) {
        validateRequest(request);

        ZoneId zoneId = ZoneId.systemDefault();
        Instant startInstant = request.getStartDate().atStartOfDay(zoneId).toInstant();
        Instant endInstant = request.getEndDate().atTime(LocalTime.MAX).atZone(zoneId).toInstant();

        List<Object[]> rawData = orderRepository.getDailyRevenueReportData(startInstant, endInstant);
        List<RevenueReportRowDto> items = reportMapper.toContinuousRevenueReportRows(
                rawData,
                request.getStartDate(),
                request.getEndDate()
        );

        return reportMapper.toRevenueReportResponse(request.getStartDate(), request.getEndDate(), items);
    }

    @Override
    @Transactional(readOnly = true)
    public ExportedReportFile exportReport(ReportFilterRequest request) {
        validateRequest(request);

        ZoneId zoneId = ZoneId.systemDefault();
        Instant startInstant = request.getStartDate().atStartOfDay(zoneId).toInstant();
        Instant endInstant = request.getEndDate().atTime(LocalTime.MAX).atZone(zoneId).toInstant();

        List<Object[]> rawData = orderRepository.getDailyRevenueReportData(startInstant, endInstant);
        if (rawData.isEmpty()) {
            throw new ResourceNotFoundException("No data found for the selected period. Report generation cancelled.");
        }

        List<RevenueReportRowDto> items = reportMapper.toContinuousRevenueReportRows(
                rawData,
                request.getStartDate(),
                request.getEndDate()
        );

        RevenueReportResponse report = reportMapper.toRevenueReportResponse(
                request.getStartDate(),
                request.getEndDate(),
                items
        );

        String filename = String.format("revenue_report_%s_%s.csv",
                request.getStartDate().toString().replace("-", ""),
                request.getEndDate().toString().replace("-", ""));

        try {
            byte[] content = csvReportExporter.export(report);
            return reportMapper.toExportedFile(filename, CSV_CONTENT_TYPE, content);
        } catch (IOException e) {
            log.error("Failed to generate CSV revenue report file: {}", e.getMessage(), e);
            throw new BadRequestException("An error occurred while generating the report. Please try again later.");
        }
    }

    private void validateRequest(ReportFilterRequest request) {
        if (request == null) {
            throw new BadRequestException("Request cannot be null.");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new BadRequestException("Start date and end date are required.");
        }
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new BadRequestException("Start Date cannot be greater than End Date.");
        }
        if (request.getEndDate().isAfter(LocalDate.now())) {
            throw new BadRequestException("End date cannot be in the future.");
        }
        if (ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) > 365) {
            throw new BadRequestException("Date range cannot exceed 365 days.");
        }
    }
}
