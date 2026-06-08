package com.garbo.core.service.AdminAnalytics;

import com.garbo.api.dto.ZoneCollectionDTO;
import com.garbo.core.repository.ZoneCollectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ZoneCollectionService {

    private final ZoneCollectionRepository repo;

    public List<ZoneCollectionDTO> getZoneCollection(String filter, String council) {

        // 1. NORMALIZE
        if (filter == null) filter = "DAILY";
        filter = filter.toUpperCase();

        // 2. DATE RANGE
        LocalDateTime startDate;
        switch (filter) {
            case "WEEKLY":  startDate = LocalDateTime.now().minusDays(7);  break;
            case "MONTHLY": startDate = LocalDateTime.now().minusDays(30); break;
            default:        startDate = LocalDate.now().atStartOfDay();    // DAILY
        }

        // 3. QUERY
        boolean hasCouncil = council != null && !council.isBlank();

        List<Object[]> raw = hasCouncil
                ? repo.getCollectedByZoneAndCouncil(startDate, council)
                : repo.getCollectedByZone(startDate);

        // 4. MAP
        return raw.stream()
                .map(row -> {
                    String zone     = row[0] != null ? row[0].toString() : "Unknown";
                    long collected  = row[1] != null ? ((Number) row[1]).longValue() : 0L;
                    return new ZoneCollectionDTO("Zone " + zone, collected);
                })
                .collect(Collectors.toList());
    }
}