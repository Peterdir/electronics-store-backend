package com.ecommerce.backend.modules.report.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExportedReportFile {
    private String filename;
    private String contentType;
    private byte[] content;
}
