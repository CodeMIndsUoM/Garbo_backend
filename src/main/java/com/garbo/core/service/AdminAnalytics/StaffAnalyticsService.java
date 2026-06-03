package com.garbo.core.service.AdminAnalytics;

import com.garbo.api.dto.staffAnalyzeDTOs.StaffAnalyticsResponseDTO;
import com.garbo.api.dto.staffAnalyzeDTOs.StaffSummaryDTO;
import com.garbo.api.dto.staffAnalyzeDTOs.ZoneStaffDTO;
import com.garbo.core.repository.BinCollectorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffAnalyticsService {

    private final BinCollectorRepository repo;

    public StaffAnalyticsResponseDTO getAnalytics(String councilId) {

        boolean filtered = councilId != null && !councilId.isBlank();

        // ── 1. Summary ────────────────────────────────────────────────────────
        List<Object[]> summaryList = filtered
            ? repo.getSummaryByCouncil(councilId)
            : repo.getSummary();

        Object[] s = (summaryList != null && !summaryList.isEmpty())
            ? summaryList.get(0)
            : new Object[]{0, 0, 0, 0.0};

        long   total          = toLong(s[0]);
        long   onDuty         = toLong(s[1]);
        long   onLeave        = toLong(s[2]);
        double avgPerformance = toDouble(s[3]);
        double attendanceRate = total > 0
            ? Math.round((onDuty * 1000.0 / total)) / 10.0
            : 0.0;

        StaffSummaryDTO summary = StaffSummaryDTO.builder()
            .totalStaff(total)
            .onDutyCount(onDuty)
            .onLeaveCount(onLeave)
            .attendanceRate(attendanceRate)
            .avgPerformance(Math.round(avgPerformance * 10.0) / 10.0)
            .build();

        // ── 2. Zone breakdown ─────────────────────────────────────────────────
        List<Object[]> zoneRows = filtered
            ? repo.getZoneBreakdownByCouncil(councilId)
            : repo.getZoneBreakdown();

        List<ZoneStaffDTO> zoneData = new ArrayList<>();
        for (Object[] row : zoneRows) {
            String zone        = String.valueOf(row[0]);
            long   staffCount  = toLong(row[1]);
            double performance = Math.round(toDouble(row[2]) * 10.0) / 10.0;

            zoneData.add(ZoneStaffDTO.builder()
                .zone(zone)
                .staff(staffCount)
                .performance(performance)
                .build());
        }

        return StaffAnalyticsResponseDTO.builder()
            .summary(summary)
            .zoneData(zoneData)
            .build();
    }

    private long toLong(Object val) {
        return val instanceof Number ? ((Number) val).longValue() : 0L;
    }

    private double toDouble(Object val) {
        return val instanceof Number ? ((Number) val).doubleValue() : 0.0;
    }
}