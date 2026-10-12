package com.ecommerce.backend.modules.report.controller;

import com.ecommerce.backend.modules.report.dto.request.ReportFilterRequest;
import com.ecommerce.backend.modules.report.dto.response.ExportedReportFile;
import com.ecommerce.backend.modules.report.dto.response.RevenueReportResponse;
import com.ecommerce.backend.modules.report.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class AdminReportController {

    private final ReportService reportService;

    @GetMapping("/preview")
    public ResponseEntity<RevenueReportResponse> previewReport(
            @Valid @ModelAttribute ReportFilterRequest request
    ) {
        return ResponseEntity.ok(reportService.previewReport(request));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportReport(
            @Valid @ModelAttribute ReportFilterRequest request
    ) {
        ExportedReportFile exportedFile = reportService.exportReport(request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(exportedFile.getContentType()));
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(exportedFile.getFilename(), StandardCharsets.UTF_8)
                        .build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(exportedFile.getContent());
    }
}
