package com.garbo.api.dto.websocket;

import lombok.Data;

import java.util.List;

@Data
public class RouteOptimizeRequestPayload {
    private String sessionId;
    private Long userId;
    private int vehicleCount;
    private int[] vehicleCapacities;
    private double depotLat;
    private double depotLng;
    private List<Long> selectedBinIds;
}
