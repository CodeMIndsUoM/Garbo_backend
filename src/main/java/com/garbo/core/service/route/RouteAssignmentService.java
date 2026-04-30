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

/**
 * Persists a completed route optimization to the database.
 *
 * Flow:
 *   1. Admin selects bins + team on the map and presses "Generate Route"
 *   2. RouteSessionController calls RouteSessionService.optimizeAndBroadcast()
 *      → returns a READY snapshot with the optimized RouteResponseDTO
 *   3. RouteSessionController then calls RouteAssignmentService.persist()
 *      → saves RouteSession, RouteAssignment (+collectors), RouteVehicleRoute,
 *        and RouteBinStop rows so the route survives beyond the in-memory session
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
    private final DriverRepository          driverRepository;
    private final BinCollectorRepository    collectorRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─────────────────────────────────────────────────────────────────────────
    // MAIN ENTRY POINT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Persist a fully optimized route snapshot to the database.
     *
     * @param request  original request carrying team details (vehicleId, driverId, collectorIds)
     * @param snapshot READY snapshot returned by RouteSessionService.optimizeAndBroadcast()
     */
    @Transactional
    public void persist(RouteAssignmentRequestDTO request, RouteSessionSnapshotDTO snapshot) {

        validateSnapshot(snapshot);
        validateRequest(request);

        String sessionId = snapshot.sessionId;

        // 1. Save (or update) the RouteSession row
        saveRouteSession(sessionId, snapshot, request);

        // 2. Save the team assignment
        saveAssignment(sessionId, request);

        // 3. Wipe any previous vehicle routes + stops for this session
        //    (handles recompute: old data replaced cleanly)
        vehicleRouteRepository.deleteBySessionId(sessionId);

        // 4. Save vehicle routes + bin stops
        saveVehicleRoutes(sessionId, snapshot);

        log.info("Route persisted: sessionId={}, vehicleRoutes={}, user={}",
                sessionId,
                snapshot.route != null ? getRouteMap(snapshot).size() : 0,
                request.getUserId());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REAL-TIME: mark a single bin stop as COLLECTED
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called by BinCollectionRealtimeService when a BIN_COLLECTED WebSocket
     * message arrives. Marks the matching stop row COLLECTED and records the time.
     *
     * @return true if a row was updated, false if not found or already collected
     */
    @Transactional
    public boolean markBinCollected(String sessionId, Long binId) {
        return binStopRepository
                .findBySessionIdAndBinId(sessionId, binId)
                .map(stop -> {
                    int updated = binStopRepository.markCollected(stop.getId(), LocalDateTime.now());
                    if (updated > 0) {
                        log.info("Bin {} marked COLLECTED in session {}", binId, sessionId);
                        return true;
                    }
                    log.warn("Bin {} in session {} was already COLLECTED or not PENDING", binId, sessionId);
                    return false;
                })
                .orElseGet(() -> {
                    log.warn("No stop found for binId={} in sessionId={}", binId, sessionId);
                    return false;
                });
    }

    /**
     * Mark a bin stop as SKIPPED.
     */
    @Transactional
    public boolean markBinSkipped(String sessionId, Long binId) {
        return binStopRepository
                .findBySessionIdAndBinId(sessionId, binId)
                .map(stop -> {
                    int updated = binStopRepository.markSkipped(stop.getId());
                    return updated > 0;
                })
                .orElse(false);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PROGRESS QUERY
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a status → count map for a session, e.g.
     * { "PENDING": 8, "COLLECTED": 4, "SKIPPED": 1 }
     */
    public Map<String, Long> getProgressSummary(String sessionId) {
        List<Object[]> rows = binStopRepository.countByStatusForSession(sessionId);
        Map<String, Long> summary = new java.util.LinkedHashMap<>();
        for (Object[] row : rows) {
            summary.put((String) row[0], (Long) row[1]);
        }
        return summary;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private void saveRouteSession(String sessionId,
                                   RouteSessionSnapshotDTO snapshot,
                                   RouteAssignmentRequestDTO request) {

        RouteSession session = routeSessionRepository
                .findById(sessionId)
                .orElse(new RouteSession());

        session.setSessionId(sessionId);
        session.setUserId(snapshot.userId);
        session.setStatus(snapshot.status);
        session.setTrigger(snapshot.trigger);
        session.setVersion(snapshot.version);
        session.setSelectedBinIds(toJson(request.getSelectedBinIds()));

        routeSessionRepository.save(session);
    }

    private void saveAssignment(String sessionId, RouteAssignmentRequestDTO request) {

        // Remove any previous assignment for this session (idempotent re-run)
        routeAssignmentRepository.findBySessionId(sessionId)
                .ifPresent(routeAssignmentRepository::delete);

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Vehicle not found: " + request.getVehicleId()));

        Driver driver = driverRepository.findById(request.getDriverId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Driver not found: " + request.getDriverId()));

        List<BinCollector> collectors = new ArrayList<>();
        for (Long cId : request.getCollectorIds()) {
            BinCollector c = collectorRepository.findById(cId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Collector not found: " + cId));
            collectors.add(c);
        }

        RouteAssignment assignment = new RouteAssignment();
        assignment.setSessionId(sessionId);
        assignment.setVehicle(vehicle);
        assignment.setDriver(driver);
        assignment.setCollectors(collectors);

        routeAssignmentRepository.save(assignment);
    }

    @SuppressWarnings("unchecked")
    private void saveVehicleRoutes(String sessionId, RouteSessionSnapshotDTO snapshot) {

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

            // Save first so the ID is available for the stops
            vehicleRoute = vehicleRouteRepository.save(vehicleRoute);

            // Build bin stops
            List<Map<String, Object>> binSequence =
                    (List<Map<String, Object>>) vr.get("binSequence");

            if (binSequence == null || binSequence.isEmpty()) {
                log.warn("Vehicle route {} in session {} has no bin stops", vehicleKey, sessionId);
                continue;
            }

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

    // ─────────────────────────────────────────────────────────────────────────
    // VALIDATION
    // ─────────────────────────────────────────────────────────────────────────

    private void validateSnapshot(RouteSessionSnapshotDTO snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("Route snapshot is null");
        }
        if (!"READY".equalsIgnoreCase(snapshot.status)) {
            throw new IllegalStateException(
                    "Cannot persist a route that is not READY. Current status: " + snapshot.status);
        }
        if (snapshot.route == null) {
            throw new IllegalStateException("Snapshot is READY but has no route data");
        }
    }

    private void validateRequest(RouteAssignmentRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Request is null");
        }
        if (!request.hasValidTeam()) {
            throw new IllegalArgumentException(
                    "Invalid team: vehicleId, driverId, and at least 2 collectorIds are required");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TYPE COERCION HELPERS
    // (snapshot.route is Object — comes through Jackson as a LinkedHashMap)
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> getRouteMap(RouteSessionSnapshotDTO snapshot) {
        try {
            // snapshot.route may be a RouteResponseDTO or a LinkedHashMap depending
            // on how it was stored. Convert via ObjectMapper to a consistent Map.
            Map<String, Object> root = objectMapper.convertValue(snapshot.route, Map.class);
            Object routes = root.get("routes");
            if (routes == null) {
                throw new IllegalStateException("Route snapshot is missing 'routes' map");
            }
            return (Map<String, Object>) routes;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse route data: " + e.getMessage(), e);
        }
    }

    private Integer toInt(Object value) {
        if (value == null) return null;
        if (value instanceof Integer i) return i;
        if (value instanceof Number n) return n.intValue();
        return Integer.parseInt(value.toString());
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Long l) return l;
        if (value instanceof Number n) return n.longValue();
        return Long.parseLong(value.toString());
    }

    private Double toDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Double d) return d;
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
}