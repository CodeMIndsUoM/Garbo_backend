package com.garbo.core.service.route;

import com.garbo.api.dto.AutoRoutePreviewResponseDTO;
import com.garbo.api.dto.AutoRoutePreviewResponseDTO.FleetSummaryDTO;
import com.garbo.api.dto.DraftRouteDTO;
import com.garbo.core.entity.Bin;
import com.garbo.core.entity.Vehicle;
import com.garbo.core.repository.BinRepository;
import com.garbo.core.service.zone.ZoneClusteringService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Selects bins and splits them into capacity-feasible route drafts (no ML).
 */
@Service
@RequiredArgsConstructor
public class AutoRouteService {

    private static final int DEFAULT_MAX_BINS = 25;

    private final BinRepository binRepository;
    private final RouteAssignmentService routeAssignmentService;
    private final ZoneClusteringService zoneClusteringService;

    @Transactional(readOnly = true)
    public AutoRoutePreviewResponseDTO preview(String council, List<String> minFillStatus, boolean useZones) {
        if (council == null || council.isBlank()) {
            throw new IllegalArgumentException("council is required");
        }

        List<String> statuses = (minFillStatus != null && !minFillStatus.isEmpty())
                ? minFillStatus
                : List.of("full", "half");

        List<Bin> eligible = binRepository.findAllByCouncil(council).stream()
                .filter(b -> b.getLatitude() != null && b.getLongitude() != null)
                .filter(b -> matchesFillStatus(b.getStatus(), statuses))
                .sorted(binPriorityComparator())
                .collect(Collectors.toCollection(ArrayList::new));

        List<Vehicle> vehicles = routeAssignmentService.getAvailableVehicles(council);

        int totalMaxBins = vehicles.stream().mapToInt(this::resolveMaxBins).sum();
        int largestVehicleBins = vehicles.stream()
                .mapToInt(this::resolveMaxBins)
                .max()
                .orElse(DEFAULT_MAX_BINS);

        List<String> warnings = new ArrayList<>();
        List<DraftRouteDTO> drafts = new ArrayList<>();

        // Step 1: auto-select bins and split into geographic route groups (K-means on coordinates).
        int chunkSize = Math.max(largestVehicleBins, DEFAULT_MAX_BINS);
        List<List<Bin>> routeGroups = useZones
                ? zoneClusteringService.splitIntoRouteGroups(eligible, chunkSize)
                : greedyChunks(eligible, chunkSize);

        int draftIndex = 0;
        for (List<Bin> chunk : routeGroups) {
            if (chunk.isEmpty()) {
                continue;
            }
            String zone = chunk.stream()
                    .map(Bin::getZone)
                    .filter(z -> z != null && !z.isBlank())
                    .findFirst()
                    .orElse(null);
            drafts.add(new DraftRouteDTO(
                    "d" + (++draftIndex),
                    chunk.stream().map(Bin::getId).toList(),
                    chunk.size(),
                    zone
            ));
        }

        // Step 2: validate fleet can cover generated routes (admin assigns vehicle per draft).
        int requiredCapacity = drafts.stream().mapToInt(DraftRouteDTO::getBinCount).sum();
        if (requiredCapacity > totalMaxBins && !drafts.isEmpty()) {
            warnings.add("Fleet total capacity (" + totalMaxBins + " bins) is less than required ("
                    + requiredCapacity + "). Assign largest vehicles or add more vehicles.");
        }
        for (DraftRouteDTO draft : drafts) {
            boolean anyFits = vehicles.stream()
                    .anyMatch(v -> resolveMaxBins(v) >= draft.getBinCount());
            if (!anyFits) {
                warnings.add("Route " + draft.getDraftId() + " has " + draft.getBinCount()
                        + " bins — no single vehicle has enough capacity");
            }
        }
        if (eligible.isEmpty()) {
            warnings.add("No bins need collection for the selected fill statuses");
        }
        if (vehicles.isEmpty()) {
            warnings.add("No available vehicles for this council — generate preview only");
        }

        return new AutoRoutePreviewResponseDTO(
                eligible.size(),
                new FleetSummaryDTO(vehicles.size(), totalMaxBins),
                drafts,
                warnings
        );
    }

    private List<List<Bin>> greedyChunks(List<Bin> eligible, int chunkSize) {
        List<List<Bin>> groups = new ArrayList<>();
        for (int i = 0; i < eligible.size(); i += chunkSize) {
            groups.add(new ArrayList<>(eligible.subList(i, Math.min(i + chunkSize, eligible.size()))));
        }
        return groups;
    }

    private Comparator<Bin> binPriorityComparator() {
        return Comparator
                .comparingInt((Bin b) -> fillPriority(b.getStatus()))
                .thenComparing(b -> b.getPriority() != null ? b.getPriority() : "medium",
                        Comparator.comparingInt(this::priorityRank));
    }

    private int fillPriority(String status) {
        String norm = normalizeStatus(status);
        if ("full".equals(norm)) return 0;
        if ("half".equals(norm)) return 1;
        return 2;
    }

    private int priorityRank(String priority) {
        if (priority == null) return 1;
        return switch (priority.toLowerCase(Locale.ROOT)) {
            case "high" -> 0;
            case "low" -> 2;
            default -> 1;
        };
    }

    private boolean matchesFillStatus(String status, List<String> allowed) {
        String norm = normalizeStatus(status);
        return allowed.stream()
                .map(this::normalizeStatus)
                .anyMatch(norm::equals);
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            return "notchecked";
        }
        return status.toLowerCase(Locale.ROOT).replace("_", "");
    }

    int resolveMaxBins(Vehicle vehicle) {
        if (vehicle.getMaxBins() != null && vehicle.getMaxBins() > 0) {
            return vehicle.getMaxBins();
        }
        if (vehicle.getCapacity() != null && vehicle.getCapacity() > 0) {
            return vehicle.getCapacity().intValue();
        }
        return DEFAULT_MAX_BINS;
    }
}
