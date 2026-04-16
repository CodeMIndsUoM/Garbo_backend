package com.garbo.api.dto;

import java.util.List;
import java.util.Map;

public class RouteResponseDTO {
    public int totalVehiclesUsed;
    public Map<Integer, VehicleRoute> routes;  // vehicleId -> route details

    public RouteResponseDTO(int totalVehiclesUsed, Map<Integer, VehicleRoute> routes) {
        this.totalVehiclesUsed = totalVehiclesUsed;
        this.routes = routes;
    }

    public static class VehicleRoute {
        public int vehicleId;
        public int capacity;
        public int totalBins;
        public double estimatedDurationSeconds;
        public List<BinStop> binSequence;  // ordered list of bins to visit

        public VehicleRoute(int vehicleId, int capacity, double estimatedDurationSeconds, List<BinStop> binSequence) {
            this.vehicleId = vehicleId;
            this.capacity = capacity;
            this.totalBins = binSequence.size();
            this.estimatedDurationSeconds = estimatedDurationSeconds;
            this.binSequence = binSequence;
        }
    }

    public static class BinStop {
        public int stopOrder;   // 1-based sequence number
        public long binId;
        public double lat;
        public double lng;
        public double durationFromPrevStopSeconds; // travel time from previous stop (or depot)

        public BinStop(int stopOrder, long binId, double lat, double lng, double durationFromPrevStopSeconds) {
            this.stopOrder = stopOrder;
            this.binId = binId;
            this.lat = lat;
            this.lng = lng;
            this.durationFromPrevStopSeconds = durationFromPrevStopSeconds;
        }
    }
}