package com.garbo.core.service.AdminAnalytics;


import com.garbo.api.dto.ComplaintDTOs.ComplaintAnalyticsResponseDTO;
import com.garbo.api.dto.ComplaintDTOs.ComplaintChartPointDTO;
import com.garbo.api.dto.ComplaintDTOs.ComplaintSummaryDTO;
import com.garbo.core.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComplaintAnalyticsService {

    private final ComplaintRepository repo;

    public ComplaintAnalyticsResponseDTO getAnalytics(String filter) {

        if (filter == null) filter = "TODAY";
        filter = filter.toUpperCase();

        // ── 1. KPI summary — always today ───────────────────────────────────
        List<Object[]> summaryList = repo.getTodaySummary();
        Object[] s = (summaryList != null && !summaryList.isEmpty())
                ? summaryList.get(0)
                : new Object[]{0L, 0L, 0L};

        long newCount    = toLong(s[0]);
        long inProgress  = toLong(s[1]);
        long resolved    = toLong(s[2]);
        long total       = newCount + inProgress + resolved;
        double resRate   = total > 0
                ? Math.round((resolved * 1000.0 / total)) / 10.0
                : 0.0;

        ComplaintSummaryDTO summary = ComplaintSummaryDTO.builder()
                .newCount(newCount)
                .inProgressCount(inProgress)
                .resolvedCount(resolved)
                .resolutionRate(resRate)
                .build();

        // ── 2. Chart data — varies by filter ────────────────────────────────
        List<Object[]> rawChart;
        switch (filter) {
            case "WEEK"  -> rawChart = repo.getWeekChart(LocalDateTime.now().minusDays(7));
            case "MONTH" -> rawChart = repo.getMonthChart(LocalDateTime.now().minusDays(30));
            default      -> rawChart = repo.getTodayChart();
        }

        List<ComplaintChartPointDTO> chartData = new ArrayList<>();
        if (rawChart != null) {
            for (Object[] row : rawChart) {
                chartData.add(ComplaintChartPointDTO.builder()
                        .label(String.valueOf(row[0]))
                        .newCount(toLong(row[1]))
                        .inProgress(toLong(row[2]))
                        .resolved(toLong(row[3]))
                        .build());
            }
        }

        return ComplaintAnalyticsResponseDTO.builder()
                .period(filter)
                .summary(summary)
                .chartData(chartData)
                .build();
    }

    private long toLong(Object val) {
        return val instanceof Number ? ((Number) val).longValue() : 0L;
    }
}