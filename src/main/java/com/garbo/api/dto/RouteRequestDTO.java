package com.garbo.api.dto;

import java.util.Arrays;

public class RouteRequestDTO {

    private int vehicleCount;
    private int[] vehicleCapacities;
    private double depotLat;   // Starting location latitude
    private double depotLng;   // Starting location longitude

    public RouteRequestDTO() {}

    public RouteRequestDTO(int vehicleCount, int[] vehicleCapacities, double depotLat, double depotLng) {
        this.vehicleCount = vehicleCount;
        this.vehicleCapacities = vehicleCapacities;
        this.depotLat = depotLat;
        this.depotLng = depotLng;
    }

    public int getVehicleCount() { return vehicleCount; }
    public void setVehicleCount(int vehicleCount) { this.vehicleCount = vehicleCount; }

    public int[] getVehicleCapacities() { return vehicleCapacities; }
    public void setVehicleCapacities(int[] vehicleCapacities) { this.vehicleCapacities = vehicleCapacities; }

    public double getDepotLat() { return depotLat; }
    public void setDepotLat(double depotLat) { this.depotLat = depotLat; }

    public double getDepotLng() { return depotLng; }
    public void setDepotLng(double depotLng) { this.depotLng = depotLng; }

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