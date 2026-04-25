package com.garbo.api.dto.ComplaintDTOs;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Full payload: GET /api/admin/complaintanalytics?filter=TODAY|WEEK|MONTH
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintAnalyticsResponseDTO {
    private String                     period;
    private ComplaintSummaryDTO        summary;       // KPI cards (always today)
    private List<ComplaintChartPointDTO> chartData;   // area chart points
}
