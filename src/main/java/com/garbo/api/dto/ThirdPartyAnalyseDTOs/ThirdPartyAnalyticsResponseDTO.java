package com.garbo.api.dto.ThirdPartyAnalyseDTOs;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ThirdPartyAnalyticsResponseDTO {

    private long totalRequests;
    private double completionRate;
    private Map<String, Long> slotDistribution;   // MORNING / AFTERNOON / EVENING
    private Map<String, Long> statusSummary;       // COMPLETED / ASSIGNED / CONFIRMED / OPEN
    private Map<String, Long> wasteTypeBreakdown;  // ORGANIC / PLASTIC / MIXED / METAL / GLASS
    private String filterPeriod;                   // TODAY | LAST_WEEK | LAST_MONTH | ALL
}
