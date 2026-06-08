package com.garbo.api.controller;

import com.garbo.api.dto.RouteAssignmentRequestDTO;
import com.garbo.api.dto.RouteSessionSnapshotDTO;
import com.garbo.core.repository.RouteBinStopRepository;
import com.garbo.core.entity.RouteVehicleRoute;
import com.garbo.core.entity.RouteAssignment;
import com.garbo.core.repository.RouteAssignmentRepository;
import com.garbo.core.repository.RouteVehicleRouteRepository;
import com.garbo.core.repository.UserRepository;
import com.garbo.core.entity.User;
import com.garbo.core.entity.AdminNew;
import com.garbo.core.entity.BinCollector;
import com.garbo.core.service.route.RouteAssignmentService;
import com.garbo.core.service.route.RouteSessionService;
import com.garbo.core.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for route session lifecycle.
 */
@Slf4j
@RestController
@RequestMapping("/api/route-sessions")
@CrossOrigin("*")
@RequiredArgsConstructor
public class RouteSessionController {

    private final RouteSessionService     routeSessionService;
    private final RouteAssignmentService  routeAssignmentService;
    private final RouteAssignmentRepository assignmentRepository;
    private final RouteVehicleRouteRepository vehicleRouteRepository;
    private final UserRepository          userRepository;

    @PostMapping
    public ResponseEntity<?> createRouteSession(@RequestBody RouteAssignmentRequestDTO request) {
        try {
            if (!request.hasValidDepot()) {
                return badRequest("depotLat and depotLng are required");
            }
            if (request.getSelectedBinIds() == null || request.getSelectedBinIds().isEmpty()) {
                return badRequest("At least one bin must be selected");
            }
            if (!request.hasValidTeam()) {
                return badRequest("vehicleId and driverId are required");
            }

            request.setUserId(request.getDriverId());
            RouteSessionSnapshotDTO snapshot = routeSessionService.optimizeAndBroadcast(request);
            return ResponseEntity.ok(snapshot);
        } catch (Exception e) {
            log.error("Unexpected error creating route session", e);
            return serverError("Route optimization failed: " + e.getMessage());
        }
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<?> getSnapshot(@PathVariable String sessionId) {
        try {
            RouteSessionSnapshotDTO snapshot = routeSessionService.getLatestSnapshot(UUID.fromString(sessionId));
            return ResponseEntity.ok(snapshot);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/user/{userId}/active")
    public ResponseEntity<?> getActiveSnapshotByUser(@PathVariable Long userId) {
        try {
            List<Object[]> rows;
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                String role = user.getRole();
                if (role != null && (role.equalsIgnoreCase("superadmin") || role.equalsIgnoreCase("role_superadmin"))) {
                    rows = assignmentRepository.findAllWithStatus();
                } else if (role != null && (role.equalsIgnoreCase("admin") || role.equalsIgnoreCase("role_admin"))) {
                    String council = null;
                    if (user instanceof AdminNew) {
                        council = ((AdminNew) user).getCouncil();
                    }
                    if (council != null && !council.isBlank()) {
                        rows = assignmentRepository.findAllByCouncilWithStatus(council);
                    } else {
                        rows = assignmentRepository.findAllWithStatus();
                    }
                } else {
                    rows = assignmentRepository.findAllByUserIdWithStatus(userId);
                }
            } else {
                rows = assignmentRepository.findAllByUserIdWithStatus(userId);
            }

            var result = rows.stream().map(row -> {
                RouteAssignment a = (RouteAssignment) row[0];
                String status = (String) row[1];
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", a.getId());
                map.put("sessionId", a.getSessionId());
                map.put("vehicleCode", a.getVehicle().getLicensePlate());
                map.put("driverName", a.getDriver() != null ? a.getDriver().getEmpName() : "");
                map.put("status", status);
                map.put("createdDate", a.getCreatedAt() != null ? a.getCreatedAt().toString() : "");
                return map;
            }).toList();
            return ResponseEntity.ok(Map.of("success", true, "data", result));
        } catch (Exception e) {
            log.error("Failed to fetch assignments with status", e);
            return ResponseEntity.ok(Map.of("success", true, "data", java.util.Collections.emptyList()));
        }
    }

    @DeleteMapping("/user/{userId}/clear")
    public ResponseEntity<?> clearHistory(@PathVariable Long userId) {
        try {
            routeAssignmentService.clearHistoryForUser(userId, routeSessionService);
            return ResponseEntity.ok(Map.of("success", true, "message", "Route history cleared successfully"));
        } catch (Exception e) {
            log.error("Failed to clear route history for user {}", userId, e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error", "Failed to clear route history: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AVAILABILITY — resources not in active sessions (filtered by council)
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/available-vehicles")
    public ResponseEntity<?> getAvailableVehicles(@RequestParam(required = false) String council) {
        String effectiveCouncil = (council != null && !council.isBlank())
                ? council
                : CurrentUserService.getCurrentCouncil().orElse(null);
        return ResponseEntity.ok(Map.of("success", true, "data", routeAssignmentService.getAvailableVehicles(effectiveCouncil)));
    }

    @GetMapping("/available-drivers")
    public ResponseEntity<?> getAvailableDrivers(@RequestParam(required = false) String council) {
        String effectiveCouncil = (council != null && !council.isBlank())
                ? council
                : CurrentUserService.getCurrentCouncil().orElse(null);
        return ResponseEntity.ok(Map.of("success", true, "data", routeAssignmentService.getAvailableDrivers(effectiveCouncil)));
    }

    @GetMapping("/{sessionId}/progress")
    public ResponseEntity<?> getProgress(@PathVariable String sessionId) {
        try {
            Map<String, Long> summary = routeAssignmentService.getProgressSummary(UUID.fromString(sessionId));
            long total     = summary.values().stream().mapToLong(Long::longValue).sum();
            long collected = summary.getOrDefault("COLLECTED", 0L);
            
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("sessionId",  sessionId);
            response.put("total",      total);
            response.put("collected",  collected);
            response.put("percentComplete", total > 0 ? Math.round((collected * 100.0) / total) : 0);
            response.put("breakdown",  summary);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return serverError("Failed to fetch progress: " + e.getMessage());
        }
    }

    @PatchMapping("/{sessionId}/bins/{binId}/collect")
    public ResponseEntity<?> collectBin(@PathVariable String sessionId, @PathVariable Long binId) {
        try {
            boolean updated = routeAssignmentService.markBinCollected(UUID.fromString(sessionId), binId);
            return ResponseEntity.ok(Map.of("sessionId", sessionId, "binId", binId, "status", updated ? "COLLECTED" : "NOT_UPDATED"));
        } catch (Exception e) {
            return serverError("Failed to mark bin collected: " + e.getMessage());
        }
    }

    @PatchMapping("/{sessionId}/bins/{binId}/skip")
    public ResponseEntity<?> skipBin(@PathVariable String sessionId, @PathVariable Long binId) {
        try {
            boolean updated = routeAssignmentService.markBinSkipped(UUID.fromString(sessionId), binId);
            return ResponseEntity.ok(Map.of("sessionId", sessionId, "binId", binId, "status", updated ? "SKIPPED" : "NOT_UPDATED"));
        } catch (Exception e) {
            return serverError("Failed to mark bin skipped: " + e.getMessage());
        }
    }

    @GetMapping("/{sessionId}/routes")
    public ResponseEntity<?> getPersistedRoutes(@PathVariable String sessionId) {
        try {
            var vehicleRoutes = vehicleRouteRepository
                    .findBySessionIdWithStops(UUID.fromString(sessionId))
                    .stream()
                    .map(RouteVehicleRoute::toDTO)
                    .toList();
            return ResponseEntity.ok(vehicleRoutes);
        } catch (Exception e) {
            log.error("Failed to fetch routes for session {}", sessionId, e);
            return serverError("Failed to fetch routes: " + e.getMessage());
        }
    }

    @GetMapping("/{sessionId}/assignment")
    public ResponseEntity<?> getAssignment(@PathVariable String sessionId) {
        try {
            return assignmentRepository.findBySessionIdWithCollectors(UUID.fromString(sessionId))
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    private ResponseEntity<Map<String, String>> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    private ResponseEntity<Map<String, String>> serverError(String message) {
        return ResponseEntity.internalServerError().body(Map.of("error", message));
    }
}
