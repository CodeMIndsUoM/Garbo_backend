package com.garbo.api.dto.binAnalyzeDTOs;


import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class BinAnalyticsResponseDTO {

    private long totalBins;
    private long urgentBins;
    private double avgFillLevel;

    private List<ZoneAnalyticsDTO> zoneData;
}