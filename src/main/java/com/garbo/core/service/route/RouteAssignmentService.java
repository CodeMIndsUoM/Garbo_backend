package com.garbo.core.service.route;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garbo.api.dto.RouteAssignmentRequestDTO;
import com.garbo.api.dto.RouteSessionSnapshotDTO;
import com.garbo.core.entity.*;
import com.garbo.core.repository.*;
import com.garbo.core.service.notification.NotificationPublisher;
import com.garbo.infrastructure.websocket.RouteCollectionBroadcaster;
import com.garbo.infrastructure.websocket.TaskAlertBroadcaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final UserRepository            userRepository;
    private final RouteCollectionBroadcaster routeCollectionBroadcaster;
    private final com.garbo.core.service.field_staff.BinService binService;
    private final TaskAlertBroadcaster taskAlertBroadcaster;
    private final NotificationPublisher notificationPublisher;
    private final com.garbo.core.service.security.SystemIncidentService systemIncidentService;
    private final com.garbo.core.repository.ComplaintRepository complaintRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void persist(RouteAssignmentRequestDTO request, RouteSessionSnapshotDTO snapshot) {
        validateSnapshot(snapshot);
        validateRequest(request);

        UUID sessionId = UUID.fromString(snapshot.sessionId);

        saveRouteSession(sessionId, snapshot, request);
        saveAssignment(sessionId, request);

        // Cache existing statuses and timestamps before deleting old route stops
        Map<Long, String> existingStatuses = new java.util.HashMap<>();
        Map<Long, LocalDateTime> existingCollectedAt = new java.util.HashMap<>();
        try {
            List<RouteVehicleRoute> existingRoutes = vehicleRouteRepository.findBySessionIdWithStops(sessionId);
            if (existingRoutes != null) {
                for (RouteVehicleRoute vr : existingRoutes) {
                    if (vr.getBinStops() != null) {
                        for (RouteBinStop stop : vr.getBinStops()) {
                            if (stop.getBinId() != null) {
                                existingStatuses.put(stop.getBinId(), stop.getStatus());
                                if (stop.getCollectedAt() != null) {
                                    existingCollectedAt.put(stop.getBinId(), stop.getCollectedAt());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to cache existing route statuses: {}", e.getMessage());
        }

        binStopRepository.deleteBySessionId(sessionId);
        vehicleRouteRepository.deleteBySessionId(sessionId);
        saveVehicleRoutes(sessionId, snapshot, existingStatuses, existingCollectedAt);

        // Mark bins as assigned
        if (request.getSelectedBinIds() != null) {
            for (Long binId : request.getSelectedBinIds()) {
                if (binId != null && binId > 0) {
                    binRepository.updateAssignedStatus(binId, true);
                    systemIncidentService.logIncident(
                        "BIN_ASSIGNMENT",
                        binId.toString(),
                        "Bin ID " + binId + " marked as assigned for route session " + sessionId
                    );
                }
            }
        }

        log.info("Route persisted: sessionId={}, vehicleRoutes={}, user={}",
                sessionId,
                snapshot.route != null ? getRouteMap(snapshot).size() : 0,
                request.getUserId());

        int binCount = request.getSelectedBinIds() != null ? request.getSelectedBinIds().size() : 0;
        taskAlertBroadcaster.notifyCollectorRouteAssigned(
                request.getUserId(),
                sessionId.toString(),
                binCount,
                request.getVehicleId());
        notificationPublisher.routeAssigned(request.getUserId(), sessionId.toString(), binCount);
    }

    @Transactional
    public boolean markBinCollected(UUID sessionId, Long binId) {
        return binStopRepository
                .findBySessionIdAndBinId(sessionId, binId)
                .map(stop -> {
                    int updated = binStopRepository.markCollected(stop.getId(), LocalDateTime.now());
                    if (updated > 0) {
                        log.info("Bin {} marked COLLECTED in session {}", binId, sessionId);
                        if (binId != null && binId > 0) {
                            binRepository.findById(binId).ifPresent(bin -> {
                                if (bin.getBinCode() != null && bin.getBinCode().startsWith("COMPLAINT-")) {
                                    try {
                                        Long complaintId = Long.parseLong(bin.getBinCode().replace("COMPLAINT-", ""));
                                        complaintRepository.findById(complaintId).ifPresent(c -> {
                                            c.setStatus("RESOLVED");
                                            c.setResolutionNotes("Resolved by collection route");
                                            complaintRepository.save(c);
                                        });
                                    } catch (Exception e) {
                                        log.error("Failed to parse complaint ID from binCode: {}", bin.getBinCode());
                                    }
                                }
                            });
                            binService.resetBinAfterCollection(binId);
                        }
                        routeCollectionBroadcaster.broadcastBinStatusUpdate(sessionId, binId, "COLLECTED");
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
                    if (updated > 0) {
                        routeCollectionBroadcaster.broadcastBinStatusUpdate(sessionId, binId, "SKIPPED");
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    @Transactional
    public boolean markBinPending(UUID sessionId, Long binId) {
        return binStopRepository
                .findBySessionIdAndBinId(sessionId, binId)
                .map(stop -> {
                    int updated = binStopRepository.markPending(stop.getId());
                    if (updated > 0) {
                        routeCollectionBroadcaster.broadcastBinStatusUpdate(sessionId, binId, "PENDING");
                        return true;
                    }
                    return false;
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
        // Allow vehicles that are either "available" or already "on_route" (to allow same driver multi-route assignments)
        return all.stream()
                .filter(v -> "available".equalsIgnoreCase(v.getStatus()) || "on_route".equalsIgnoreCase(v.getStatus()))
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
        
        BinCollector driver = collectorRepository.findById(request.getDriverId()).orElseThrow();
        
        // Find if driver already has a vehicle assigned (on_route)
        Vehicle vehicle = vehicleRepository.findFirstByAssignedDriverId(request.getDriverId())
                .filter(v -> "on_route".equalsIgnoreCase(v.getStatus()))
                .orElseGet(() -> {
                    if (request.getVehicleId() == null) {
                        throw new IllegalArgumentException("vehicleId is required when driver is not already assigned to a vehicle");
                    }
                    return vehicleRepository.findById(request.getVehicleId()).orElseThrow();
                });
        
        // Automatically update vehicle status and sync driver
        vehicle.setStatus("on_route");
        vehicle.setAssignedDriverId(request.getDriverId());
        vehicleRepository.save(vehicle);
        
        RouteAssignment assignment = new RouteAssignment();
        assignment.setSessionId(sessionId);
        assignment.setVehicle(vehicle);
        assignment.setDriver(driver);
        assignment.setCollectors(new ArrayList<>());
        routeAssignmentRepository.save(assignment);

        systemIncidentService.logIncident(
            "ROUTE_ASSIGNMENT",
            sessionId.toString(),
            "Route session " + sessionId + " assigned to driver " + driver.getEmpName() + " (ID " + driver.getEmpId() + ") and vehicle " + vehicle.getLicensePlate()
        );
        systemIncidentService.logIncident(
            "VEHICLE_ASSIGNMENT",
            vehicle.getId().toString(),
            "Driver " + driver.getEmpName() + " (ID " + driver.getEmpId() + ") assigned to vehicle " + vehicle.getLicensePlate() + " for route session " + sessionId
        );
    }

    @Transactional
    public void completeRouteSession(UUID sessionId) {
        routeSessionRepository.findById(sessionId).ifPresent(session -> {
            session.setStatus("COMPLETED");
            routeSessionRepository.save(session);
        });

        routeAssignmentRepository.findBySessionId(sessionId).ifPresent(assignment -> {
            Vehicle vehicle = assignment.getVehicle();
            if (vehicle != null) {
                vehicle.setStatus("available");
                vehicleRepository.save(vehicle);
            }
        });
    }

    private void saveVehicleRoutes(UUID sessionId, RouteSessionSnapshotDTO snapshot,
                                   Map<Long, String> existingStatuses,
                                   Map<Long, LocalDateTime> existingCollectedAt) {
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
                Long binId = toLong(stopMap.get("binId"));
                stop.setBinId(binId);
                stop.setLat(toDouble(stopMap.get("lat")));
                stop.setLng(toDouble(stopMap.get("lng")));
                stop.setDurationFromPrevSeconds(toDouble(stopMap.get("durationFromPrevStopSeconds")));
                
                String status = "PENDING";
                if (existingStatuses != null && existingStatuses.containsKey(binId)) {
                    status = existingStatuses.get(binId);
                }
                stop.setStatus(status);
                
                if (existingCollectedAt != null && existingCollectedAt.containsKey(binId)) {
                    stop.setCollectedAt(existingCollectedAt.get(binId));
                }
                
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

    /**
     * Clears route history using the same scope as GET /user/{userId}/active:
     * superadmin → all sessions; council admin → council sessions; others → own sessions.
     */
    @Transactional
    public void clearHistoryForUser(Long userId, RouteSessionService sessionService) {
        List<RouteSession> sessions = resolveSessionsForClear(userId);
        for (RouteSession session : sessions) {
            deleteSessionCompletely(session, sessionService);
        }
        sessionService.deleteByUser(userId);
        if (isSuperAdmin(userId)) {
            sessionService.clearAllSessions();
        }
    }

    private List<RouteSession> resolveSessionsForClear(Long userId) {
        return userRepository.findById(userId)
                .map(user -> {
                    String role = user.getRole();
                    if (isSuperAdminRole(role)) {
                        return routeSessionRepository.findAll();
                    }
                    if (isAdminRole(role)) {
                        String council = null;
                        if (user instanceof AdminNew admin) {
                            council = admin.getCouncil();
                        }
                        if (council != null && !council.isBlank()) {
                            return findSessionsByCouncil(council);
                        }
                        return routeSessionRepository.findAll();
                    }
                    return routeSessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
                })
                .orElseGet(() -> routeSessionRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    private List<RouteSession> findSessionsByCouncil(String council) {
        Set<UUID> sessionIds = new LinkedHashSet<>();
        for (Object[] row : routeAssignmentRepository.findAllByCouncilWithStatus(council)) {
            RouteAssignment assignment = (RouteAssignment) row[0];
            sessionIds.add(assignment.getSessionId());
        }
        List<RouteSession> sessions = new ArrayList<>();
        for (UUID sessionId : sessionIds) {
            routeSessionRepository.findById(sessionId).ifPresent(sessions::add);
        }
        return sessions;
    }

    private void deleteSessionCompletely(RouteSession session, RouteSessionService sessionService) {
        UUID sessionId = session.getSessionId();

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

        routeAssignmentRepository.findBySessionId(sessionId).ifPresent(assignment -> {
            Vehicle vehicle = assignment.getVehicle();
            if (vehicle != null) {
                vehicle.setStatus("available");
                vehicle.setAssignedDriverId(null);
                vehicleRepository.save(vehicle);
            }
            routeAssignmentRepository.delete(assignment);
        });

        binStopRepository.deleteBySessionId(sessionId);
        vehicleRouteRepository.deleteBySessionId(sessionId);
        routeSessionRepository.delete(session);
        sessionService.deleteSession(sessionId);
    }

    private boolean isSuperAdmin(Long userId) {
        return userRepository.findById(userId)
                .map(u -> isSuperAdminRole(u.getRole()))
                .orElse(false);
    }

    private boolean isSuperAdminRole(String role) {
        return role != null
                && (role.equalsIgnoreCase("superadmin") || role.equalsIgnoreCase("role_superadmin"));
    }

    private boolean isAdminRole(String role) {
        return role != null
                && (role.equalsIgnoreCase("admin") || role.equalsIgnoreCase("role_admin"));
    }
}
