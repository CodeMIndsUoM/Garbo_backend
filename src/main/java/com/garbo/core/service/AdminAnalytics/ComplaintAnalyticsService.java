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

    public ComplaintAnalyticsResponseDTO getAnalytics(String filter, String councilId) {

        if (filter == null) filter = "TODAY";
        filter = filter.toUpperCase();

        boolean filtered = councilId != null && !councilId.isBlank();

        // ── 1. KPI summary — always today ────────────────────────────────────
        List<Object[]> summaryList = filtered
            ? repo.getTodaySummaryByCouncil(councilId)
            : repo.getTodaySummary();

        Object[] s = (summaryList != null && !summaryList.isEmpty())
            ? summaryList.get(0)
            : new Object[]{0L, 0L};

        long pendingCount  = toLong(s[0]);
        long acceptedCount = toLong(s[1]);
        long total         = pendingCount + acceptedCount;
        double resRate     = total > 0
            ? Math.round((acceptedCount * 1000.0 / total)) / 10.0
            : 0.0;

        ComplaintSummaryDTO summary = ComplaintSummaryDTO.builder()
            .pendingCount(pendingCount)
            .acceptedCount(acceptedCount)
            .resolutionRate(resRate)
            .build();

        // ── 2. Chart data — varies by filter ─────────────────────────────────
        List<Object[]> rawChart;

        if (filtered) {
            rawChart = switch (filter) {
                case "WEEK"  -> repo.getWeekChartByCouncil(LocalDateTime.now().minusDays(7), councilId);
                case "MONTH" -> repo.getMonthChartByCouncil(LocalDateTime.now().minusDays(30), councilId);
                default      -> repo.getTodayChartByCouncil(councilId);
            };
        } else {
            rawChart = switch (filter) {
                case "WEEK"  -> repo.getWeekChart(LocalDateTime.now().minusDays(7));
                case "MONTH" -> repo.getMonthChart(LocalDateTime.now().minusDays(30));
                default      -> repo.getTodayChart();
            };
        }

        List<ComplaintChartPointDTO> chartData = new ArrayList<>();
        if (rawChart != null) {
            for (Object[] row : rawChart) {
                chartData.add(ComplaintChartPointDTO.builder()
                    .label(String.valueOf(row[0]))
                    .pendingCount(toLong(row[1]))
                    .acceptedCount(toLong(row[2]))
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