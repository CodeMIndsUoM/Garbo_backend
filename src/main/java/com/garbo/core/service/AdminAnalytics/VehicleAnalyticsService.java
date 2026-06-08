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

    private static final String STATUS_AVAILABLE   = "available";
    private static final String STATUS_ON_ROUTE    = "on_route";
    private static final String STATUS_MAINTENANCE = "maintenance";

    public VehicleAnalyticsDTO getAnalytics(String councilId) {
        boolean filtered = councilId != null && !councilId.isBlank();

        long totalFleet  = filtered
            ? vehicleRepository.countByIsActiveTrueAndAssignedCouncil(councilId)
            : vehicleRepository.countByIsActiveTrue();

        long onRoute     = filtered
            ? vehicleRepository.countByStatusAndIsActiveTrueAndAssignedCouncil(STATUS_ON_ROUTE, councilId)
            : vehicleRepository.countByStatusAndIsActiveTrue(STATUS_ON_ROUTE);

        long available   = filtered
            ? vehicleRepository.countByStatusAndIsActiveTrueAndAssignedCouncil(STATUS_AVAILABLE, councilId)
            : vehicleRepository.countByStatusAndIsActiveTrue(STATUS_AVAILABLE);

        long maintenance = filtered
            ? vehicleRepository.countByStatusAndIsActiveTrueAndAssignedCouncil(STATUS_MAINTENANCE, councilId)
            : vehicleRepository.countByStatusAndIsActiveTrue(STATUS_MAINTENANCE);

        List<Vehicle> vehicles = filtered
            ? vehicleRepository.findAllByIsActiveTrueAndAssignedCouncilOrderByIdAsc(councilId)
            : vehicleRepository.findAllByIsActiveTrueOrderByIdAsc();

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

    public VehicleAnalyticsDTO getAnalyticsByStatus(String statusFilter, String councilId) {
        boolean filtered = councilId != null && !councilId.isBlank();

        long totalFleet  = filtered
            ? vehicleRepository.countByIsActiveTrueAndAssignedCouncil(councilId)
            : vehicleRepository.countByIsActiveTrue();

        long onRoute     = filtered
            ? vehicleRepository.countByStatusAndIsActiveTrueAndAssignedCouncil(STATUS_ON_ROUTE, councilId)
            : vehicleRepository.countByStatusAndIsActiveTrue(STATUS_ON_ROUTE);

        long available   = filtered
            ? vehicleRepository.countByStatusAndIsActiveTrueAndAssignedCouncil(STATUS_AVAILABLE, councilId)
            : vehicleRepository.countByStatusAndIsActiveTrue(STATUS_AVAILABLE);

        long maintenance = filtered
            ? vehicleRepository.countByStatusAndIsActiveTrueAndAssignedCouncil(STATUS_MAINTENANCE, councilId)
            : vehicleRepository.countByStatusAndIsActiveTrue(STATUS_MAINTENANCE);

        List<Vehicle> vehicles;

        boolean allStatuses = statusFilter == null || statusFilter.equalsIgnoreCase("all");
        String dbStatus = allStatuses ? null : statusFilter.toLowerCase().replace(" ", "_");

        if (allStatuses) {
            vehicles = filtered
                ? vehicleRepository.findAllByIsActiveTrueAndAssignedCouncilOrderByIdAsc(councilId)
                : vehicleRepository.findAllByIsActiveTrueOrderByIdAsc();
        } else {
            vehicles = filtered
                ? vehicleRepository.findAllByStatusAndIsActiveTrueAndAssignedCouncilOrderByIdAsc(dbStatus, councilId)
                : vehicleRepository.findAllByStatusAndIsActiveTrueOrderByIdAsc(dbStatus);
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