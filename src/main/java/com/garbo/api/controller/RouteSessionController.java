package com.garbo.api.controller;

import com.garbo.api.dto.RouteSessionCreateRequestDTO;
import com.garbo.api.dto.RouteSessionCreateResponseDTO;
import com.garbo.api.dto.RouteSessionSnapshotDTO;
import com.garbo.core.service.route.RouteSessionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/route-sessions")
public class RouteSessionController {
    @Autowired
    private RouteSessionService routeSessionService;

    @PostMapping
    public ResponseEntity<?> createOrReplaceSession(@RequestBody RouteSessionCreateRequestDTO request) {
        try {
            RouteSessionCreateResponseDTO response = routeSessionService.createOrReplaceSession(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body("Failed to create route session: " + ex.getMessage());
        }
    }

    @GetMapping("/{sessionId}/latest")
    public ResponseEntity<?> getLatestBySession(@PathVariable String sessionId) {
        try {
            RouteSessionSnapshotDTO latest = routeSessionService.getLatestSnapshot(sessionId);
            return ResponseEntity.ok(latest);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @GetMapping("/users/{userId}/latest")
    public ResponseEntity<?> getLatestByUser(@PathVariable Long userId) {
        try {
            RouteSessionSnapshotDTO latest = routeSessionService.getLatestSnapshotByUser(userId);
            return ResponseEntity.ok(latest);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PostMapping("/{sessionId}/recompute")
    public ResponseEntity<?> recomputeBySession(@PathVariable String sessionId) {
        try {
            RouteSessionSnapshotDTO latest = routeSessionService.triggerRecompute(sessionId);
            return ResponseEntity.ok(latest);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PostMapping("/users/{userId}/recompute")
    public ResponseEntity<?> recomputeByUser(@PathVariable Long userId) {
        try {
            RouteSessionSnapshotDTO latest = routeSessionService.triggerRecomputeByUser(userId);
            return ResponseEntity.ok(latest);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteBySession(@PathVariable String sessionId) {
        routeSessionService.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteByUser(@PathVariable Long userId) {
        routeSessionService.deleteSessionByUser(userId);
        return ResponseEntity.noContent().build();
    }
}
