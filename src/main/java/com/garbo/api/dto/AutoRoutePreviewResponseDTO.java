package com.garbo.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AutoRoutePreviewResponseDTO {
    private int totalBinsNeedingCollection;
    private FleetSummaryDTO fleetSummary;
    private List<DraftRouteDTO> draftRoutes;
    private List<String> warnings;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FleetSummaryDTO {
        private int availableVehicles;
        private int totalMaxBins;
    }
}
