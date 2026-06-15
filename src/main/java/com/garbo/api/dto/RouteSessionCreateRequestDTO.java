package com.garbo.api.dto;

import lombok.Data;

import java.util.Arrays;
import java.util.List;

@Data
public class RouteSessionCreateRequestDTO {

    private String sessionId;
    private Long userId;
    private int vehicleCount;
    private int[] vehicleCapacities;
    private double depotLat;
    private double depotLng;
    private List<Long> selectedBinIds;

    /** Optional complaint IDs to inject as ad-hoc route stops (virtual bins). */
    private List<Long> complaintIds;

    // ── Team assignment fields ───────────────────────────────────────────────
    private Long vehicleId;
    private Long driverId;

    /**
     * Optional support labour IDs. The bin collector/driver is the required
     * assignee for the route.
     */
    private List<Long> collectorIds;
    // ────────────────────────────────────────────────────────────────────────

    public boolean hasValidDepot() {
        return depotLat != 0.0 && depotLng != 0.0;
    }

    /**
     * Returns true when the route has the required vehicle and bin collector.
     * Additional collectors are optional support labour.
     */
    public boolean hasValidTeam() {
        return vehicleId != null
                && driverId != null;
    }

    public int[] getValidatedCapacities() {
        if (vehicleCount <= 0) {
            vehicleCount = 1;
        }
        if (vehicleCapacities != null && vehicleCapacities.length == vehicleCount) {
            return vehicleCapacities;
        }
        int[] defaults = new int[vehicleCount];
        Arrays.fill(defaults, 25);
        return defaults;
    }
}
