package com.garbo.core.service.AdminAnalytics;

import com.garbo.api.dto.VehicleAnalyticsDTOs.VehicleAnalyticsDTO;
import com.garbo.api.dto.VehicleAnalyticsDTOs.VehicleAnalyticsDTO.VehicleRowDTO;
import com.garbo.core.entity.Vehicle;
import com.garbo.core.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehicleAnalyticsService {

    private final VehicleRepository vehicleRepository;

    // DB status values
    private static final String STATUS_AVAILABLE   = "available";
    private static final String STATUS_ON_ROUTE    = "on_route";
    private static final String STATUS_MAINTENANCE = "maintenance";

    public VehicleAnalyticsDTO getAnalytics() {

        //  KPIs 
        long totalFleet   = vehicleRepository.countByIsActiveTrue();
        long onRoute      = vehicleRepository.countByStatusAndIsActiveTrue(STATUS_ON_ROUTE);
        long available    = vehicleRepository.countByStatusAndIsActiveTrue(STATUS_AVAILABLE);
        long maintenance  = vehicleRepository.countByStatusAndIsActiveTrue(STATUS_MAINTENANCE);

        //  Fleet table 
        List<Vehicle> vehicles = vehicleRepository.findAllByIsActiveTrueOrderByIdAsc();

        List<VehicleRowDTO> rows = vehicles.stream()
            .map(v -> new VehicleRowDTO(
                String.valueOf(v.getId()),
                v.getLicensePlate(),
                v.getType(),
                formatStatus(v.getStatus())   // "on_route" → "On Route"
            ))
            .collect(Collectors.toList());

        return new VehicleAnalyticsDTO(totalFleet, onRoute, available, maintenance, rows);
    }

    //  Filtered fleet list (used when frontend passes a status filter) 
    public VehicleAnalyticsDTO getAnalyticsByStatus(String statusFilter) {

        long totalFleet  = vehicleRepository.countByIsActiveTrue();
        long onRoute     = vehicleRepository.countByStatusAndIsActiveTrue(STATUS_ON_ROUTE);
        long available   = vehicleRepository.countByStatusAndIsActiveTrue(STATUS_AVAILABLE);
        long maintenance = vehicleRepository.countByStatusAndIsActiveTrue(STATUS_MAINTENANCE);

        List<Vehicle> vehicles;

        if (statusFilter == null || statusFilter.equalsIgnoreCase("all")) {
            vehicles = vehicleRepository.findAllByIsActiveTrueOrderByIdAsc();
        } else {
            vehicles = vehicleRepository.findAllByStatusAndIsActiveTrueOrderByIdAsc(
                statusFilter.toLowerCase().replace(" ", "_")  // "On Route" → "on_route"
            );
        }

        List<VehicleRowDTO> rows = vehicles.stream()
            .map(v -> new VehicleRowDTO(
                String.valueOf(v.getId()),
                v.getLicensePlate(),
                v.getType(),
                formatStatus(v.getStatus())
            ))
            .collect(Collectors.toList());

        return new VehicleAnalyticsDTO(totalFleet, onRoute, available, maintenance, rows);
    }

    //  Convert DB status to display label 
    private String formatStatus(String dbStatus) {
        if (dbStatus == null) return "Unknown";
        return switch (dbStatus.toLowerCase()) {
            case "on_route"    -> "On Route";
            case "available"   -> "Available";
            case "maintenance" -> "Maintenance";
            case "inactive"    -> "Inactive";
            default            -> dbStatus;
        };
    }
}
