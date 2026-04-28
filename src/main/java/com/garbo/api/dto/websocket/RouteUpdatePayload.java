package com.garbo.api.dto.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.Map;

/**
 * Payload for real-time route updates (server → client).
 * Contains optimized route and bin collection order.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteUpdatePayload {
    
    @JsonProperty("sessionId")
    private String sessionId;
    
    @JsonProperty("userId")
    private Long userId;
    
    @JsonProperty("totalVehiclesUsed")
    private int totalVehiclesUsed;
    
    @JsonProperty("routes")
    private Map<Integer, VehicleRoute> routes;  // vehicleId -> route details
    
    @JsonProperty("updatedAt")
    private long updatedAt;
    
    /**
     * Single vehicle route with bin sequence.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleRoute {
        
        @JsonProperty("vehicleId")
        private int vehicleId;
        
        @JsonProperty("capacity")
        private int capacity;
        
        @JsonProperty("totalBins")
        private int totalBins;
        
        @JsonProperty("estimatedDurationSeconds")
        private double estimatedDurationSeconds;
        
        @JsonProperty("binSequence")
        private List<BinStop> binSequence;
    }
    
    /**
     * Single bin stop in the route.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BinStop {
        
        @JsonProperty("stopOrder")
        private int stopOrder;  // 1, 2, 3, ...
        
        @JsonProperty("binId")
        private long binId;
        
        @JsonProperty("lat")
        private double lat;
        
        @JsonProperty("lng")
        private double lng;
        
        @JsonProperty("durationFromPrevStopSeconds")
        private double durationFromPrevStopSeconds;  // Travel time from previous stop
        
        @JsonProperty("address")
        private String address;  // Optional: street address for display
    }
}
