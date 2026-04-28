package com.garbo.api.dto.performance;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyPerformancePoint {
    private String date;
    private int totalCollected;
    private int routesDone;
    private double averageRouteTimeSeconds;
    private double efficiencyPercent;
    private int assignedBinsTotal;
    private int missedBinsTotal;
}
