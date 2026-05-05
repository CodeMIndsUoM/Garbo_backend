package com.garbo.api.dto;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class RouteVehicleRouteDTO {
    private Long id;
    private UUID sessionId;
    private String vehicleKey;
    private Integer capacity;
    private Integer totalBins;
    private Double estimatedDurationSeconds;
    private List<RouteBinStopDTO> binStops;
}