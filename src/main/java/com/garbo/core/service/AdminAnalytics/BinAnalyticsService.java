package com.garbo.core.service.AdminAnalytics;

import com.garbo.api.dto.binAnalyzeDTOs.BinAnalyticsResponseDTO;
import com.garbo.api.dto.binAnalyzeDTOs.ZoneAnalyticsDTO;
import com.garbo.core.entity.Bin;
import com.garbo.core.repository.BinRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class BinAnalyticsService {

    @Autowired
    private BinRepository binRepository;

    public BinAnalyticsResponseDTO getAnalytics(String councilId) {

        boolean filtered = councilId != null && !councilId.isBlank();

        // ── Load bins ─────────────────────────────────────────────────────────
        List<Bin> allBins = filtered
            ? binRepository.findAllByCouncil(councilId)
            : binRepository.findAllValidBins();

        // ── KPIs ──────────────────────────────────────────────────────────────
        long totalBins  = allBins.size();
        long urgentBins = filtered
            ? binRepository.countFullBinsByCouncil(councilId)
            : binRepository.countFullBins();

        // ── Zones — dynamic from DB ───────────────────────────────────────────
        List<String> zones = filtered
            ? binRepository.findDistinctZonesByCouncil(councilId)
            : binRepository.findDistinctZones();

        // Sort zones naturally (1, 2, 3 ... 10 instead of 1, 10, 2)
        zones.sort(Comparator.comparingInt(z -> {
            try { return Integer.parseInt(z.trim()); }
            catch (NumberFormatException e) { return Integer.MAX_VALUE; }
        }));

        // ── Zone breakdown ────────────────────────────────────────────────────
        List<ZoneAnalyticsDTO> zoneData = new ArrayList<>();

        for (String zone : zones) {
            List<Bin> bins = allBins.stream()
                .filter(b -> zone.equalsIgnoreCase(b.getZone()))
                .toList();

            int empty      = 0;
            int half       = 0;
            int full       = 0;
            int notChecked = 0;
            int high       = 0;
            int medium     = 0;
            int low        = 0;

            for (Bin b : bins) {
                String status = b.getStatus() == null ? "notChecked" : b.getStatus().toLowerCase();
                switch (status) {
                    case "empty"      -> empty++;
                    case "half"       -> half++;
                    case "full"       -> full++;
                    default           -> notChecked++;
                }

                String priority = b.getPriority();
                if ("HIGH".equalsIgnoreCase(priority))        high++;
                else if ("MEDIUM".equalsIgnoreCase(priority)) medium++;
                else                                           low++;
            }

            zoneData.add(new ZoneAnalyticsDTO(
                zone,
                bins.size(),
                empty, half, full, notChecked,
                high, medium, low
            ));
        }

        return new BinAnalyticsResponseDTO(totalBins, urgentBins, zoneData);
    }
}