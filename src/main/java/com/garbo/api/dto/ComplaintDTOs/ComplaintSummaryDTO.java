package com.garbo.api.dto.ComplaintDTOs;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * KPI cards — counts for today
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintSummaryDTO {
    private long newCount;
    private long inProgressCount;
    private long resolvedCount;
    private double resolutionRate;   // resolved / total * 100
}