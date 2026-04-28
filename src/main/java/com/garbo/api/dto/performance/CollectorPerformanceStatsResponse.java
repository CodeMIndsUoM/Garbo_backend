package com.garbo.api.dto.performance;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectorPerformanceStatsResponse {
    private Long userId;
    private String fromDate;
    private int totalCollected;
    private int routesDone;
    private double averageRouteTimeSeconds;
    private double efficiencyPercent;
    private int assignedBinsTotal;
    private int missedBinsTotal;
    private List<DailyPerformancePoint> timeSeries;
}
