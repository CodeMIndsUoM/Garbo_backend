package com.garbo.api.dto;

import lombok.Data;
import java.util.List;

@Data
public class RouteRequestDTO {

    private Long userId;

    private int vehicleCount;

    private int[] vehicleCapacities;

    private double depotLat;

    private double depotLng;

    private List<Long> selectedBinIds;
}







/*package com.garbo.api.dto;

import java.util.Arrays;
import java.util.List;
import lombok.Data;

@Data
public class RouteRequestDTO {
    private Long userId;
    private int vehicleCount;
    private int[] vehicleCapacities;
    private double depotLat;   // Starting location latitude
    private double depotLng;   // Starting location longitude
    private List<Long> selectedBinIds;

    public int[] getValidatedCapacities() {
        if (vehicleCount <= 0) vehicleCount = 1;
        if (vehicleCapacities != null && vehicleCapacities.length == vehicleCount) {
            return vehicleCapacities;
        }
        int[] defaultCaps = new int[vehicleCount];
        Arrays.fill(defaultCaps, 100);
        return defaultCaps;
    }

    public boolean hasValidDepot() {
        return depotLat != 0.0 && depotLng != 0.0;
    }
}

*/