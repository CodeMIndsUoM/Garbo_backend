package com.garbo.core.service.AdminAnalytics;

import com.garbo.api.dto.binReportAnalyticsDTOs.BinReportAnalyticsDTO;
import com.garbo.api.dto.binReportAnalyticsDTOs.BinReportAnalyticsDTO.DailyCount;
import com.garbo.api.dto.binReportAnalyticsDTOs.BinReportAnalyticsDTO.HourlyCount;
import com.garbo.core.repository.BinReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BinReportAnalyticsService {

    private final BinReportRepository binReportRepository;

    public BinReportAnalyticsDTO getAnalytics(String council) {

        // Time boundaries
        LocalDate today      = LocalDate.now();
        LocalDateTime dayStart   = today.atStartOfDay();
        LocalDateTime dayEnd     = today.plusDays(1).atStartOfDay();
        LocalDateTime weekStart  = today.minusDays(6).atStartOfDay();

        boolean hasCouncil = council != null && !council.isBlank();

        // KPIs
        long totalReports    = hasCouncil
            ? binReportRepository.countReportsBetweenByCouncil(dayStart, dayEnd, council)
            : binReportRepository.countReportsBetween(dayStart, dayEnd);

        long affectedBins    = hasCouncil
            ? binReportRepository.countDistinctBinsBetweenByCouncil(dayStart, dayEnd, council)
            : binReportRepository.countDistinctBinsBetween(dayStart, dayEnd);

        long uniqueReporters = hasCouncil
            ? binReportRepository.countDistinctReportersBetweenByCouncil(dayStart, dayEnd, council)
            : binReportRepository.countDistinctReportersBetween(dayStart, dayEnd);

        // Hourly frequency (today)
        List<Object[]> hourlyRaw = hasCouncil
            ? binReportRepository.countByHourBetweenByCouncil(dayStart, dayEnd, council)
            : binReportRepository.countByHourBetween(dayStart, dayEnd);

        Map<Integer, Long> hourMap = new LinkedHashMap<>();
        for (int h = 0; h < 24; h++) hourMap.put(h, 0L);

        for (Object[] row : hourlyRaw) {
            int  hour  = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            hourMap.put(hour, count);
        }

        List<HourlyCount> hourlyList = new ArrayList<>();
        hourMap.forEach((h, c) ->
            hourlyList.add(new HourlyCount(String.format("%02d:00", h), c))
        );

        // Daily frequency (last 7 days)
        List<Object[]> dailyRaw = hasCouncil
            ? binReportRepository.countByDayBetweenByCouncil(weekStart, dayEnd, council)
            : binReportRepository.countByDayBetween(weekStart, dayEnd);

        Map<LocalDate, Long> dayMap = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) dayMap.put(today.minusDays(i), 0L);

        for (Object[] row : dailyRaw) {
            LocalDate date = ((Date) row[0]).toLocalDate();
            long count     = ((Number) row[1]).longValue();
            if (dayMap.containsKey(date)) dayMap.put(date, count);
        }

        List<DailyCount> dailyList = new ArrayList<>();
        dayMap.forEach((date, count) -> {
            String label = date.equals(today)
                ? "Today"
                : date.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            dailyList.add(new DailyCount(label, count));
        });

        return new BinReportAnalyticsDTO(
            totalReports,
            affectedBins,
            uniqueReporters,
            hourlyList,
            dailyList
        );
    }
}