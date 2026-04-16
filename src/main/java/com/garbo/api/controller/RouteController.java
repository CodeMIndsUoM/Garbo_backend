package com.garbo.api.controller;

import com.garbo.api.dto.RouteRequestDTO;
import com.garbo.api.dto.RouteSessionCreateRequestDTO;
import com.garbo.api.dto.RouteSessionSnapshotDTO;
import com.garbo.core.service.route.RouteSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/routes")
@CrossOrigin("*")
public class RouteController {

    @Autowired
    private RouteSessionService routeSessionService;

    private static final long DEFAULT_USER_ID = 42L;

    /**
     * Legacy-compatible endpoint (still used by admin dashboard / Flutter client)
     * Internally converts request → session-based optimization pipeline
     * and broadcasts real-time updates via WebSocket
     */
    @PostMapping("/optimize")
    public ResponseEntity<?> optimizeRoutes(@RequestBody RouteRequestDTO request) {

        try {
            // 1. Convert legacy request → session request
            RouteSessionCreateRequestDTO sessionRequest = new RouteSessionCreateRequestDTO();

            // user handling (fallback if not provided)
            sessionRequest.setUserId(
                    request.getUserId() != null
                            ? request.getUserId()
                            : DEFAULT_USER_ID
            );

            // routing configuration
            sessionRequest.setVehicleCount(request.getVehicleCount());
            sessionRequest.setVehicleCapacities(request.getVehicleCapacities());

            // depot location
            sessionRequest.setDepotLat(request.getDepotLat());
            sessionRequest.setDepotLng(request.getDepotLng());

            // IMPORTANT: selected bins from admin dashboard
            sessionRequest.setSelectedBinIds(request.getSelectedBinIds());

            // 2. Call session-based optimization engine
            RouteSessionSnapshotDTO snapshot =
                    routeSessionService.optimizeAndBroadcast(sessionRequest);

            // 3. Return latest snapshot (also pushed via WebSocket internally)
            return ResponseEntity.ok(snapshot);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .status(500)
                    .body("Error optimizing routes: " + e.getMessage());
        }
    }
}