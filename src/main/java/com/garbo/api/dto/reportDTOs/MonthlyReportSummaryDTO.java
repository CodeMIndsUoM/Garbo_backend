package com.garbo.api.dto.reportDTOs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lightweight DTO used for the reports list page.
 * Does NOT include the full snapshot — keeps the list response small.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyReportSummaryDTO {

    private Long id;
    private String title;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String status;           // "COMPLETED" | "PROCESSING" | "FAILED"
    private LocalDateTime createdAt;
    private Integer fileSizeKb;

    // ── Computed display helpers ──────────────────────────────────────────────
    private String periodLabel;      // e.g. "October 2025"
    private String fileSizeDisplay;  // e.g. "2.4 MB"
}