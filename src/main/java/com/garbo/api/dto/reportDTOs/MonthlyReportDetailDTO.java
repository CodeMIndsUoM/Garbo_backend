package com.garbo.api.dto.reportDTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Full report DTO returned by GET /api/admin/reports/{id}.
 * Includes the complete deserialized snapshot for frontend rendering / print-to-PDF.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyReportDetailDTO {

    private Long id;
    private String title;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String status;
    private LocalDateTime createdAt;
    private Integer fileSizeKb;
    private String periodLabel;
    private String fileSizeDisplay;

    
    private ReportSnapshotPayload snapshot;
}