package com.garbo.api.dto;

import java.util.Arrays;
import java.util.List;
import lombok.Data;

@Data
public class RouteSessionCreateRequestDTO {
    private String sessionId;
    private Long userId;
    private int vehicleCount;
    private int[] vehicleCapacities;
    private double depotLat;
    private double depotLng;
    private List<Long> selectedBinIds;

    public boolean hasValidDepot() {
        return depotLat != 0.0 && depotLng != 0.0;
    }

    public int[] getValidatedCapacities() {
        if (vehicleCount <= 0) {
            vehicleCount = 1;
        }
        if (vehicleCapacities != null && vehicleCapacities.length == vehicleCount) {
            return vehicleCapacities;
        }
        int[] defaults = new int[vehicleCount];
        Arrays.fill(defaults, 100);
        return defaults;
    }
}
