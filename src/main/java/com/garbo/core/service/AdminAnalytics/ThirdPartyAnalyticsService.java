package com.garbo.core.service.AdminAnalytics;

import com.garbo.api.dto.ThirdPartyAnalyseDTOs.ThirdPartyAnalyticsResponseDTO;
import com.garbo.core.repository.CollectionRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ThirdPartyAnalyticsService {

    private final CollectionRequestRepository repo;

    private static final List<String> SLOT_KEYS       = Arrays.asList("MORNING", "AFTERNOON", "EVENING");
    private static final List<String> STATUS_KEYS     = Arrays.asList("OPEN", "ASSIGNED", "IN_PROGRESS", "COMPLETED", "CONFIRMED", "CANCELLED");
    private static final List<String> WASTE_TYPE_KEYS = Arrays.asList("ORGANIC", "PLASTIC", "MIXED", "METAL", "GLASS", "PAPER");

    @Transactional(readOnly = true)
    public ThirdPartyAnalyticsResponseDTO getAnalytics(String period, String councilId) {

        Instant from     = resolveFrom(period);
        boolean allTime  = (from == null);
        boolean filtered = councilId != null && !councilId.isBlank();

        // ── Totals ────────────────────────────────────────────────────────────
        long total;
        long completed;

        if (filtered) {
            total     = allTime ? repo.countByCouncil(councilId)
                                : repo.countByCreatedAtAfterAndCouncil(from, councilId);
            completed = allTime ? repo.countCompletedAllTimeAndCouncil(councilId)
                                : repo.countCompletedAfterAndCouncil(from, councilId);
        } else {
            total     = allTime ? repo.count()
                                : repo.countByCreatedAtAfter(from);
            completed = allTime ? repo.countCompletedAllTime()
                                : repo.countCompletedAfter(from);
        }

        double completionRate = total == 0 ? 0.0
                : Math.round((completed * 100.0 / total) * 10) / 10.0;

        // ── Group-by distributions ────────────────────────────────────────────
        List<Object[]> slotRows;
        List<Object[]> statusRows;
        List<Object[]> wasteRows;

        if (filtered) {
            slotRows   = allTime ? repo.countBySlotGroupedAllTimeAndCouncil(councilId)
                                 : repo.countBySlotGroupedAndCouncil(from, councilId);
            statusRows = allTime ? repo.countByStatusGroupedAllTimeAndCouncil(councilId)
                                 : repo.countByStatusGroupedAndCouncil(from, councilId);
            wasteRows  = allTime ? repo.countByWasteTypeGroupedAllTimeAndCouncil(councilId)
                                 : repo.countByWasteTypeGroupedAndCouncil(from, councilId);
        } else {
            slotRows   = allTime ? repo.countBySlotGroupedAllTime()
                                 : repo.countBySlotGrouped(from);
            statusRows = allTime ? repo.countByStatusGroupedAllTime()
                                 : repo.countByStatusGrouped(from);
            wasteRows  = allTime ? repo.countByWasteTypeGroupedAllTime()
                                 : repo.countByWasteTypeGrouped(from);
        }

        return ThirdPartyAnalyticsResponseDTO.builder()
                .totalRequests(total)
                .completionRate(completionRate)
                .slotDistribution(toOrderedMap(slotRows, SLOT_KEYS))
                .statusSummary(toOrderedMap(statusRows, STATUS_KEYS))
                .wasteTypeBreakdown(toOrderedMap(wasteRows, WASTE_TYPE_KEYS))
                .filterPeriod(period == null ? "ALL" : period.toUpperCase())
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Instant resolveFrom(String period) {
        if (period == null) return null;
        return switch (period.toUpperCase()) {
            case "TODAY"      -> Instant.now().truncatedTo(ChronoUnit.DAYS);
            case "LAST_WEEK"  -> Instant.now().minus(7,  ChronoUnit.DAYS);
            case "LAST_MONTH" -> Instant.now().minus(30, ChronoUnit.DAYS);
            default           -> null;
        };
    }

    private Map<String, Long> toOrderedMap(List<Object[]> rows, List<String> orderedKeys) {
        Map<String, Long> raw = rows.stream()
                .collect(Collectors.toMap(
                        r -> r[0].toString(),
                        r -> ((Number) r[1]).longValue()
                ));

        Map<String, Long> result = new LinkedHashMap<>();
        for (String key : orderedKeys) {
            result.put(key, raw.getOrDefault(key, 0L));
        }
        return result;
    }
}