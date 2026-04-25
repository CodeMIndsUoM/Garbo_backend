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

    // Expected enum string values — keeps the maps ordered & complete even when
    // a bucket has 0 entries (no row returned from DB for that value).
    private static final List<String> SLOT_KEYS       = Arrays.asList("MORNING", "AFTERNOON", "EVENING");
    private static final List<String> STATUS_KEYS     = Arrays.asList("COMPLETED", "ASSIGNED", "CONFIRMED", "OPEN");
    private static final List<String> WASTE_TYPE_KEYS = Arrays.asList("ORGANIC", "PLASTIC", "MIXED", "METAL", "GLASS");

    /**
     * @param period  "TODAY" | "LAST_WEEK" | "LAST_MONTH" | "ALL"  (case-insensitive)
     */
    @Transactional(readOnly = true)
    public ThirdPartyAnalyticsResponseDTO getAnalytics(String period) {

        Instant from = resolveFrom(period);
        boolean allTime = (from == null);

        // ── Raw totals ────────────────────────────────────────────────────────
        long total     = allTime ? repo.count()               : repo.countByCreatedAtAfter(from);
        long completed = allTime ? repo.countCompletedAllTime(): repo.countCompletedAfter(from);

        double completionRate = total == 0 ? 0.0
                : Math.round((completed * 100.0 / total) * 10) / 10.0;   // 1 decimal place

        // ── Group-by distributions ────────────────────────────────────────────
        List<Object[]> slotRows   = allTime ? repo.countBySlotGroupedAllTime()
                                            : repo.countBySlotGrouped(from);
        List<Object[]> statusRows = allTime ? repo.countByStatusGroupedAllTime()
                                            : repo.countByStatusGrouped(from);
        List<Object[]> wasteRows  = allTime ? repo.countByWasteTypeGroupedAllTime()
                                            : repo.countByWasteTypeGrouped(from);

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

    /**
     * Returns the start-of-window Instant, or null for "all time".
     */
    private Instant resolveFrom(String period) {
        if (period == null) return null;
        return switch (period.toUpperCase()) {
            case "TODAY"     -> Instant.now().truncatedTo(ChronoUnit.DAYS);
            case "LAST_WEEK" -> Instant.now().minus(7,  ChronoUnit.DAYS);
            case "LAST_MONTH"-> Instant.now().minus(30, ChronoUnit.DAYS);
            default          -> null;   // "ALL" or unknown → no filter
        };
    }

    /**
     * Converts raw Object[][]{enumString, count} rows into an ordered map,
     * filling in 0 for any expected key that has no DB row.
     */
    private Map<String, Long> toOrderedMap(List<Object[]> rows, List<String> orderedKeys) {
        // Build a lookup from the raw results (enum .name() → count)
        Map<String, Long> raw = rows.stream()
                .collect(Collectors.toMap(
                        r -> r[0].toString(),   // enum .toString() == .name()
                        r -> ((Number) r[1]).longValue()
                ));

        Map<String, Long> result = new LinkedHashMap<>();
        for (String key : orderedKeys) {
            result.put(key, raw.getOrDefault(key, 0L));
        }
        return result;
    }
}