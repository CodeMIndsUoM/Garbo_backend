package com.garbo.api.controller;

import com.garbo.api.dto.RouteRequestDTO;
import com.garbo.api.dto.RouteSessionCreateRequestDTO;
import com.garbo.api.dto.RouteSessionSnapshotDTO;
import com.garbo.core.service.route.RouteSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/route-sessions")
@CrossOrigin("*")
public class RouteSessionController {

    @Autowired
    private RouteSessionService routeSessionService;

    /**
     * Create or replace a route session (user selects bins + vehicles)
     * Returns initial snapshot + websocket topic
     */
    @PostMapping
    public ResponseEntity<RouteSessionSnapshotDTO> createSession(
            @RequestBody RouteSessionCreateRequestDTO request
    ) {
        try {
            RouteSessionSnapshotDTO snapshot = routeSessionService.createSession(request);
            return ResponseEntity.ok(snapshot);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(null);
        }
    }

    /**
     * Get latest snapshot of a session
     */
    @GetMapping("/{sessionId}/latest")
    public ResponseEntity<RouteSessionSnapshotDTO> getLatest(
            @PathVariable String sessionId
    ) {
        try {
            RouteSessionSnapshotDTO snapshot =
                    routeSessionService.getLatestSnapshot(sessionId);

            return ResponseEntity.ok(snapshot);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(null);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Get latest snapshot for a user (active session)
     */
    @GetMapping("/users/{userId}/latest")
    public ResponseEntity<RouteSessionSnapshotDTO> getLatestByUser(
            @PathVariable Long userId
    ) {
        try {
            RouteSessionSnapshotDTO snapshot =
                    routeSessionService.getLatestSnapshotByUser(userId);

            return ResponseEntity.ok(snapshot);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(null);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Force recompute session (admin action or manual refresh)
     */
    @PostMapping("/{sessionId}/recompute")
    public ResponseEntity<RouteSessionSnapshotDTO> recompute(
            @PathVariable String sessionId
    ) {
        try {
            RouteSessionSnapshotDTO snapshot =
                    routeSessionService.recompute(sessionId);

            return ResponseEntity.ok(snapshot);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(null);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Recompute using userId (active session)
     */
    @PostMapping("/users/{userId}/recompute")
    public ResponseEntity<RouteSessionSnapshotDTO> recomputeByUser(
            @PathVariable Long userId
    ) {
        try {
            RouteSessionSnapshotDTO snapshot =
                    routeSessionService.recomputeByUser(userId);

            return ResponseEntity.ok(snapshot);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(null);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(null);
        }
    }

    /**
     * Delete session
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteSession(
            @PathVariable String sessionId
    ) {
        try {
            routeSessionService.deleteSession(sessionId);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * Delete user session
     */
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteByUser(
            @PathVariable Long userId
    ) {
        try {
            routeSessionService.deleteByUser(userId);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}



