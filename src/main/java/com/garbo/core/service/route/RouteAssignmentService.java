package com.garbo.core.service.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garbo.api.dto.RouteAssignmentRequestDTO;
import com.garbo.api.dto.RouteSessionSnapshotDTO;
import com.garbo.core.entity.*;
import com.garbo.core.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persists a completed route optimization to the database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RouteAssignmentService {

    private final RouteSessionRepository    routeSessionRepository;
    private final RouteAssignmentRepository routeAssignmentRepository;
    private final RouteVehicleRouteRepository vehicleRouteRepository;
    private final RouteBinStopRepository    binStopRepository;
    private final VehicleRepository         vehicleRepository;
    private final BinCollectorRepository    collectorRepository;
    private final BinRepository             binRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void persist(RouteAssignmentRequestDTO request, RouteSessionSnapshotDTO snapshot) {
        validateSnapshot(snapshot);
        validateRequest(request);

        UUID sessionId = UUID.fromString(snapshot.sessionId);

        saveRouteSession(sessionId, snapshot, request);
        saveAssignment(sessionId, request);

        binStopRepository.deleteBySessionId(sessionId);
        vehicleRouteRepository.deleteBySessionId(sessionId);
        saveVehicleRoutes(sessionId, snapshot);

        // Mark bins as assigned
        if (request.getSelectedBinIds() != null) {
            for (Long binId : request.getSelectedBinIds()) {
                binRepository.updateAssignedStatus(binId, true);
            }
        }

        log.info("Route persisted: sessionId={}, vehicleRoutes={}, user={}",
                sessionId,
                snapshot.route != null ? getRouteMap(snapshot).size() : 0,
                request.getUserId());
    }

    @Transactional
    public boolean markBinCollected(UUID sessionId, Long binId) {
        return binStopRepository
                .findBySessionIdAndBinId(sessionId, binId)
                .map(stop -> {
                    int updated = binStopRepository.markCollected(stop.getId(), LocalDateTime.now());
                    if (updated > 0) {
                        log.info("Bin {} marked COLLECTED in session {}", binId, sessionId);
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    @Transactional
    public boolean markBinSkipped(UUID sessionId, Long binId) {
        return binStopRepository
                .findBySessionIdAndBinId(sessionId, binId)
                .map(stop -> {
                    int updated = binStopRepository.markSkipped(stop.getId());
                    return updated > 0;
                })
                .orElse(false);
    }

    public Map<String, Long> getProgressSummary(UUID sessionId) {
        List<Object[]> rows = binStopRepository.countByStatusForSession(sessionId);
        Map<String, Long> summary = new java.util.LinkedHashMap<>();
        for (Object[] row : rows) {
            summary.put((String) row[0], (Long) row[1]);
        }
        return summary;
    }

    public List<Vehicle> getAvailableVehicles(String council) {
        List<Vehicle> all = vehicleRepository.findAll();
        List<Long> busyIds = routeAssignmentRepository.findBusyVehicleIds();
        return all.stream()
                .filter(v -> "available".equalsIgnoreCase(v.getStatus()))
                .filter(v -> !busyIds.contains(v.getId()))
                .filter(v -> council == null || council.equalsIgnoreCase(v.getAssignedCouncil()))
                .toList();
    }

    public List<BinCollector> getAvailableDrivers(String council) {
        List<BinCollector> all = collectorRepository.findAll();
        return all.stream()
                .filter(d -> council == null || council.equalsIgnoreCase(d.getAssignedCouncil()))
                .toList();
    }

    private void saveRouteSession(UUID sessionId, RouteSessionSnapshotDTO snapshot, RouteAssignmentRequestDTO request) {
        RouteSession session = routeSessionRepository.findById(sessionId).orElse(new RouteSession());
        session.setSessionId(sessionId);
        session.setUserId(snapshot.userId);
        session.setStatus(snapshot.status);
        session.setTrigger(snapshot.trigger);
        session.setVersion(snapshot.version);
        session.setSelectedBinIds(toJson(request.getSelectedBinIds()));
        routeSessionRepository.save(session);
    }

    private void saveAssignment(UUID sessionId, RouteAssignmentRequestDTO request) {
        routeAssignmentRepository.findBySessionId(sessionId).ifPresent(routeAssignmentRepository::delete);
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId()).orElseThrow();
        
        // Automatically update vehicle status and sync driver
        vehicle.setStatus("on_route");
        vehicle.setAssignedDriverId(request.getDriverId());
        vehicleRepository.save(vehicle);
        
        BinCollector driver = collectorRepository.findById(request.getDriverId()).orElseThrow();
        RouteAssignment assignment = new RouteAssignment();
        assignment.setSessionId(sessionId);
        assignment.setVehicle(vehicle);
        assignment.setDriver(driver);
        assignment.setCollectors(new ArrayList<>());
        routeAssignmentRepository.save(assignment);
    }

    private void saveVehicleRoutes(UUID sessionId, RouteSessionSnapshotDTO snapshot) {
        Map<String, Object> routesMap = getRouteMap(snapshot);
        for (Map.Entry<String, Object> entry : routesMap.entrySet()) {
            String vehicleKey = entry.getKey();
            Map<String, Object> vr = (Map<String, Object>) entry.getValue();
            RouteVehicleRoute vehicleRoute = new RouteVehicleRoute();
            vehicleRoute.setSessionId(sessionId);
            vehicleRoute.setVehicleKey(vehicleKey);
            vehicleRoute.setCapacity(toInt(vr.get("capacity")));
            vehicleRoute.setTotalBins(toInt(vr.get("totalBins")));
            vehicleRoute.setEstimatedDurationSeconds(toDouble(vr.get("estimatedDurationSeconds")));
            vehicleRoute = vehicleRouteRepository.save(vehicleRoute);
            List<Map<String, Object>> binSequence = (List<Map<String, Object>>) vr.get("binSequence");
            List<RouteBinStop> stops = new ArrayList<>();
            for (Map<String, Object> stopMap : binSequence) {
                RouteBinStop stop = new RouteBinStop();
                stop.setVehicleRoute(vehicleRoute);
                stop.setStopOrder(toInt(stopMap.get("stopOrder")));
                stop.setBinId(toLong(stopMap.get("binId")));
                stop.setLat(toDouble(stopMap.get("lat")));
                stop.setLng(toDouble(stopMap.get("lng")));
                stop.setDurationFromPrevSeconds(toDouble(stopMap.get("durationFromPrevStopSeconds")));
                stop.setStatus("PENDING");
                stops.add(stop);
            }
            binStopRepository.saveAll(stops);
        }
    }

    private void validateSnapshot(RouteSessionSnapshotDTO snapshot) {
        if (snapshot == null || !"READY".equalsIgnoreCase(snapshot.status) || snapshot.route == null) {
            throw new IllegalArgumentException("Invalid snapshot");
        }
    }

    private void validateRequest(RouteAssignmentRequestDTO request) {
        if (request == null || !request.hasValidTeam()) {
            throw new IllegalArgumentException("Invalid request");
        }
        
    }

    private Map<String, Object> getRouteMap(RouteSessionSnapshotDTO snapshot) {
        Map<String, Object> root = objectMapper.convertValue(snapshot.route, Map.class);
        return (Map<String, Object>) root.get("routes");
    }

    private Integer toInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.intValue();
        return Integer.parseInt(value.toString());
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        return Long.parseLong(value.toString());
    }

    private Double toDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.doubleValue();
        return Double.parseDouble(value.toString());
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return value != null ? value.toString() : null;
        }
    }

    @Transactional
    public void clearHistoryForUser(Long userId, RouteSessionService sessionService) {
        List<RouteSession> sessions = routeSessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (RouteSession session : sessions) {
            UUID sessionId = session.getSessionId();

            // 1. Mark bins as unassigned
            if (session.getSelectedBinIds() != null) {
                try {
                    List<Long> binIds = objectMapper.readValue(
                        session.getSelectedBinIds(),
                        new com.fasterxml.jackson.core.type.TypeReference<List<Long>>() {}
                    );
                    if (binIds != null) {
                        for (Long binId : binIds) {
                            binRepository.updateAssignedStatus(binId, false);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse selectedBinIds for session clear: {}", e.getMessage());
                }
            }

            // 2. Reset vehicle status and delete assignments
            routeAssignmentRepository.findBySessionId(sessionId).ifPresent(assignment -> {
                Vehicle vehicle = assignment.getVehicle();
                if (vehicle != null) {
                    vehicle.setStatus("available");
                    vehicle.setAssignedDriverId(null);
                    vehicleRepository.save(vehicle);
                }
                routeAssignmentRepository.delete(assignment);
            });

            // 3. Delete bin stops and routes
            binStopRepository.deleteBySessionId(sessionId);
            vehicleRouteRepository.deleteBySessionId(sessionId);

            // 4. Delete the session from database
            routeSessionRepository.delete(session);

            // 5. Remove from RouteSessionService in-memory tracking
            sessionService.deleteSession(sessionId);
        }
        sessionService.deleteByUser(userId);
    }
}
