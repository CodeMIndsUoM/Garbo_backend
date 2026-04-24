package com.garbo.core.service;

import com.garbo.api.dto.Collect_analyze_dtos.ChartDataDTO;
import com.garbo.api.dto.Collect_analyze_dtos.DashboardResponseDTO;
import com.garbo.core.repository.CollectorRouteCompletionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AnalyticsService {

    @Autowired
    private CollectorRouteCompletionRepository repo;

    public DashboardResponseDTO getDashboard(String filter) {

        // =========================
        // 1. NORMALIZE FILTER
        // =========================
        if (filter == null) {
            filter = "DAY";
        }
        filter = filter.toUpperCase();

        // =========================
        // 2. DATE RANGE
        // =========================
        LocalDateTime startDate;

        switch (filter) {
            case "WEEK":
                startDate = LocalDateTime.now().minusDays(7);
                break;
            case "MONTH":
                startDate = LocalDateTime.now().minusDays(30);
                break;
            default:
                startDate = LocalDateTime.now().minusDays(1);
        }

        // =========================
        // 3. SUMMARY (SAFE)
        // getSummary returns List<Object[]> — unwrap the first row
        // =========================
        List<Object[]> summaryList = repo.getSummary(startDate);
        Object[] summary = (summaryList != null && !summaryList.isEmpty()) ? summaryList.get(0) : null;

        int assigned = 0;
        int collected = 0;
        int missed = 0;

        if (summary != null) {
            assigned  = summary[0] != null ? ((Number) summary[0]).intValue() : 0;
            collected = summary[1] != null ? ((Number) summary[1]).intValue() : 0;
            missed    = summary[2] != null ? ((Number) summary[2]).intValue() : 0;
        }

        // =========================
        // 4. CHART DATA SELECTION
        // =========================
        List<Object[]> rawData;

        if ("WEEK".equals(filter)) {
            rawData = repo.getDailyData(startDate);
        } else if ("MONTH".equals(filter)) {
            rawData = repo.getWeeklyData(startDate);
        } else {
            rawData = repo.getHourlyData(startDate);
        }

        // =========================
        // 5. CHART MAPPING (SAFE)
        // =========================
        List<ChartDataDTO> chartData = new ArrayList<>();

        if (rawData != null) {
            for (Object[] row : rawData) {
                String label = row[0] != null ? String.valueOf(row[0]) : "0";
                int a = row[1] != null ? ((Number) row[1]).intValue() : 0;
                int c = row[2] != null ? ((Number) row[2]).intValue() : 0;
                int m = row[3] != null ? ((Number) row[3]).intValue() : 0;

                chartData.add(new ChartDataDTO(label, a, c, m));
            }
        }

        // =========================
        // 6. RESPONSE
        // =========================
        return new DashboardResponseDTO(
                assigned,
                collected,
                missed,
                chartData
        );
    }
}