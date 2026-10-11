package com.ecommerce.backend.modules.report.service;

import com.ecommerce.backend.modules.report.dto.request.ReportFilterRequest;
import com.ecommerce.backend.modules.report.dto.response.ExportedReportFile;
import com.ecommerce.backend.modules.report.dto.response.RevenueReportResponse;

public interface ReportService {

    RevenueReportResponse previewReport(ReportFilterRequest request);

    ExportedReportFile exportReport(ReportFilterRequest request);
}
