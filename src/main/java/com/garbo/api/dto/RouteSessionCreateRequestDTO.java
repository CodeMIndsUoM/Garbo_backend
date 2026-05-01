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

    // ── Team assignment fields ───────────────────────────────────────────────
    private Long vehicleId;
    private Long driverId;

    /**
     * Collector emp_ids — minimum 2 required when creating a route assignment.
     */
    private List<Long> collectorIds;
    // ────────────────────────────────────────────────────────────────────────

    public boolean hasValidDepot() {
        return depotLat != 0.0 && depotLng != 0.0;
    }

    /**
     * Returns true when all team fields are present and at least 2 collectors
     * have been selected. Used by RouteAssignmentService before persisting.
     */
    public boolean hasValidTeam() {
        return vehicleId != null
                && driverId != null
                && collectorIds != null
                && collectorIds.size() >= 2;
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