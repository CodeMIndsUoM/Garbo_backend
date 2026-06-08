package com.garbo.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouncilBoundaryDTO {

    private String council;
    private double depotLat;
    private double depotLng;
    private List<CoordinatePoint> boundaryPoints;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CoordinatePoint {
        private double lat;
        private double lng;
    }
}