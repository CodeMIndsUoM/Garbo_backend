package com.garbo.api.controller;

import com.garbo.api.dto.RouteAssignmentRequestDTO;
import com.garbo.api.dto.RouteSessionSnapshotDTO;
import com.garbo.core.repository.RouteBinStopRepository;
import com.garbo.core.repository.RouteAssignmentRepository;
import com.garbo.core.repository.RouteVehicleRouteRepository;
import com.garbo.core.service.route.RouteAssignmentService;
import com.garbo.core.service.route.RouteSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST controller for route session lifecycle.
 *
 * POST /api/route-sessions
 *   — Admin presses "Generate Route" on the map.
 *   — Runs OR-Tools optimization, persists everything to DB, returns the snapshot.
 *
 * GET  /api/route-sessions/{sessionId}
 *   — Poll the latest snapshot for a session (fallback if WebSocket is unavailable).
 *
 * GET  /api/route-sessions/{sessionId}/progress
 *   — Returns PENDING / COLLECTED / SKIPPED counts for a session.
 *
 * PATCH /api/route-sessions/{sessionId}/bins/{binId}/collect
 *   — REST fallback to mark a bin stop COLLECTED (primary path is WebSocket).
 *
 * PATCH /api/route-sessions/{sessionId}/bins/{binId}/skip
 *   — Mark a bin stop SKIPPED.
 *
 * GET  /api/route-sessions/user/{userId}/active
 *   — Get the latest in-memory snapshot for a user (mirrors existing behaviour).
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
    private final RouteBinStopRepository  binStopRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE  —  main endpoint called by MapView.tsx "Generate Route" button
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 1. Validate team fields (vehicleId, driverId, collectorIds).
     * 2. Run route optimization via RouteSessionService (existing in-memory pipeline).
     * 3. Persist the READY snapshot + team assignment to the database.
     * 4. Return the snapshot so the frontend can start listening on the WebSocket topic.
     */
    @PostMapping
    public ResponseEntity<?> createRouteSession(@RequestBody RouteAssignmentRequestDTO request) {

        try {
            // ── Validate ───────────────────────────────────────────────────
            if (request.getUserId() == null || request.getUserId() <= 0) {
                return badRequest("userId is required");
            }
            if (!request.hasValidDepot()) {
                return badRequest("depotLat and depotLng are required");
            }
            if (request.getSelectedBinIds() == null || request.getSelectedBinIds().isEmpty()) {
                return badRequest("At least one bin must be selected");
            }
            if (!request.hasValidTeam()) {
                return badRequest("vehicleId, driverId, and at least 2 collectorIds are required");
            }

            // ── Optimize (existing pipeline — unchanged) ────────────────────
            RouteSessionSnapshotDTO snapshot =
                    routeSessionService.optimizeAndBroadcast(request.toSessionRequest());

            // ── Persist to DB ───────────────────────────────────────────────
            // Only persist when optimization actually succeeded.
            if ("READY".equalsIgnoreCase(snapshot.status)) {
                routeAssignmentService.persist(request, snapshot);
                log.info("Route session created and persisted: sessionId={}, user={}",
                        snapshot.sessionId, request.getUserId());
            } else {
                // PROCESSING or ERROR — still return the snapshot so the frontend
                // can subscribe to the WebSocket topic for the eventual READY update.
                log.warn("Optimization did not reach READY status immediately: status={}, sessionId={}",
                        snapshot.status, snapshot.sessionId);
            }

            return ResponseEntity.ok(snapshot);

        } catch (IllegalArgumentException e) {
            log.warn("Route session creation rejected: {}", e.getMessage());
            return badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error creating route session", e);
            return serverError("Route optimization failed: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // READ — latest in-memory snapshot (WebSocket fallback)
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/{sessionId}")
    public ResponseEntity<?> getSnapshot(@PathVariable String sessionId) {
        try {
            RouteSessionSnapshotDTO snapshot = routeSessionService.getLatestSnapshot(sessionId);
            return ResponseEntity.ok(snapshot);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/user/{userId}/active")
    public ResponseEntity<?> getActiveSnapshotByUser(@PathVariable Long userId) {
        try {
            RouteSessionSnapshotDTO snapshot = routeSessionService.getLatestSnapshotByUser(userId);
            return ResponseEntity.ok(snapshot);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PROGRESS — PENDING / COLLECTED / SKIPPED counts
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/{sessionId}/progress")
    public ResponseEntity<?> getProgress(@PathVariable String sessionId) {
        try {
            Map<String, Long> summary = routeAssignmentService.getProgressSummary(sessionId);

            long total     = summary.values().stream().mapToLong(Long::longValue).sum();
            long collected = summary.getOrDefault("COLLECTED", 0L);
            long pending   = summary.getOrDefault("PENDING",   0L);
            long skipped   = summary.getOrDefault("SKIPPED",   0L);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("sessionId",  sessionId);
            response.put("total",      total);
            response.put("collected",  collected);
            response.put("pending",    pending);
            response.put("skipped",    skipped);
            response.put("percentComplete",
                    total > 0 ? Math.round((collected * 100.0) / total) : 0);
            response.put("breakdown",  summary);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching progress for session {}", sessionId, e);
            return serverError("Failed to fetch progress: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BIN STATUS UPDATES — REST fallback (primary path is WebSocket)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Mark a specific bin stop as COLLECTED.
     * Primary path is BIN_COLLECTED WebSocket message → BinCollectionRealtimeService.
     * This endpoint is a REST fallback (e.g. collector app loses WebSocket connection).
     */
    @PatchMapping("/{sessionId}/bins/{binId}/collect")
    public ResponseEntity<?> collectBin(
            @PathVariable String sessionId,
            @PathVariable Long binId) {

        try {
            boolean updated = routeAssignmentService.markBinCollected(sessionId, binId);

            if (updated) {
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("sessionId", sessionId);
                body.put("binId",     binId);
                body.put("status",    "COLLECTED");
                return ResponseEntity.ok(body);
            } else {
                return ResponseEntity.ok(Map.of(
                        "sessionId", sessionId,
                        "binId",     binId,
                        "status",    "NOT_UPDATED",
                        "message",   "Stop not found or already collected"
                ));
            }

        } catch (Exception e) {
            log.error("Error marking bin {} collected in session {}", binId, sessionId, e);
            return serverError("Failed to mark bin collected: " + e.getMessage());
        }
    }

    /**
     * Mark a specific bin stop as SKIPPED.
     */
    @PatchMapping("/{sessionId}/bins/{binId}/skip")
    public ResponseEntity<?> skipBin(
            @PathVariable String sessionId,
            @PathVariable Long binId) {

        try {
            boolean updated = routeAssignmentService.markBinSkipped(sessionId, binId);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("sessionId", sessionId);
            body.put("binId",     binId);
            body.put("status",    updated ? "SKIPPED" : "NOT_UPDATED");

            return ResponseEntity.ok(body);

        } catch (Exception e) {
            log.error("Error marking bin {} skipped in session {}", binId, sessionId, e);
            return serverError("Failed to mark bin skipped: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ROUTE DATA — fetch persisted routes from DB
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns all vehicle routes + stops for a session from the DB.
     * Used by the "Assigned Routes" panel on the map to re-draw persisted routes.
     */
    @GetMapping("/{sessionId}/routes")
    public ResponseEntity<?> getPersistedRoutes(@PathVariable String sessionId) {
        try {
            var vehicleRoutes = vehicleRouteRepository.findBySessionIdWithStops(sessionId);

            if (vehicleRoutes.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(vehicleRoutes);

        } catch (Exception e) {
            log.error("Error fetching persisted routes for session {}", sessionId, e);
            return serverError("Failed to fetch routes: " + e.getMessage());
        }
    }

    /**
     * Returns the team assignment (vehicle + driver + collectors) for a session.
     */
    @GetMapping("/{sessionId}/assignment")
    public ResponseEntity<?> getAssignment(@PathVariable String sessionId) {
        return assignmentRepository
                .findBySessionIdWithCollectors(sessionId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private ResponseEntity<Map<String, String>> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    private ResponseEntity<Map<String, String>> serverError(String message) {
        return ResponseEntity.internalServerError().body(Map.of("error", message));
    }
}