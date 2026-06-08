package com.garbo.api.dto.VehicleAnalyticsDTOs;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VehicleAnalyticsDTO {

    // KPI cards
    private long totalFleet;        // all active vehicles (isActive = true)
    private long onRoute;           // status = 'on_route'
    private long available;         // status = 'available'
    private long maintenance;       // status = 'maintenance'

    // Fleet table
    private List<VehicleRowDTO> vehicles;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class VehicleRowDTO {
        private String vehicleId;     // vehicleCode  → "TRK-001"
        private String plate;         // licensePlate → "WP CAG-1234"
        private String type;          // type         → "Compactor"
        private String status;        // display-friendly status
    }
}