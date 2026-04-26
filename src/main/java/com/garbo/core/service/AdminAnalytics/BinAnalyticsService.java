package com.garbo.core.service.AdminAnalytics;

import com.garbo.api.dto.binAnalyzeDTOs.BinAnalyticsResponseDTO;
import com.garbo.api.dto.binAnalyzeDTOs.ZoneAnalyticsDTO;
import com.garbo.core.entity.Bin;
import com.garbo.core.repository.BinRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BinAnalyticsService {

    @Autowired
    private BinRepository binRepository;

    public BinAnalyticsResponseDTO getAnalytics() {

        // ✅ ONLY VALID ZONES
        List<String> validZones = List.of("A", "B", "C", "D", "E");

        // ✅ LOAD FILTERED DATA FROM DB (IMPORTANT)
        List<Bin> allBins = binRepository.findAllValidBins();

        // KPI CALCULATIONS
        long totalBins = allBins.size();

        long urgentBins = allBins.stream()
                .filter(b -> b.getFillLevel() > 75)
                .count();

        double avgFillLevel = allBins.stream()
                .mapToInt(Bin::getFillLevel)
                .average()
                .orElse(0.0);

        // ZONE ANALYTICS
        List<ZoneAnalyticsDTO> zoneData = new ArrayList<>();

        for (String zone : validZones) {

            List<Bin> bins = allBins.stream()
                    .filter(b -> zone.equalsIgnoreCase(b.getZone()))
                    .toList();

            int below30 = 0;
            int fill30_50 = 0;
            int fill50_75 = 0;
            int above75 = 0;

            int high = 0;
            int medium = 0;
            int low = 0;

            for (Bin b : bins) {

                int fill = b.getFillLevel();

                // Fill level ranges
                if (fill < 30) below30++;
                else if (fill < 50) fill30_50++;
                else if (fill < 75) fill50_75++;
                else above75++;

                // Priority
                if ("HIGH".equalsIgnoreCase(b.getPriority())) high++;
                else if ("MEDIUM".equalsIgnoreCase(b.getPriority())) medium++;
                else low++;
            }

            // ✅ IMPORTANT: Match frontend "Zone A"
            String displayZone = "Zone " + zone;

            zoneData.add(new ZoneAnalyticsDTO(
                    displayZone,
                    bins.size(),
                    below30,
                    fill30_50,
                    fill50_75,
                    above75,
                    high,
                    medium,
                    low
            ));
        }

        return new BinAnalyticsResponseDTO(
                totalBins,
                urgentBins,
                avgFillLevel,
                zoneData
        );
    }
}