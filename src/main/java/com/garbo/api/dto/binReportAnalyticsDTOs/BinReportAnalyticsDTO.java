package com.garbo.api.dto.binReportAnalyticsDTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BinReportAnalyticsDTO {

    // KPI cards
    private long totalReportsToday;
    private long affectedBinsToday;
    private long uniqueReportersToday;

    // Hourly frequency for today (for the area chart)
    private List<HourlyCount> reportFrequencyToday;

    // Daily frequency for last 7 days (for a weekly bar/line chart)
    private List<DailyCount> reportFrequencyLastWeek;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HourlyCount {
        private String time;   // e.g. "08:00"
        private long count;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DailyCount {
        private String day;    // as "Mon", "Tue"
        private long count;
    }
}