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
public class RouteController {
    @Autowired
    private RouteSessionService routeSessionService;

    private static final long DEFAULT_USER_ID = 42L;
    
    @PostMapping("/optimize")
    public ResponseEntity<?> optimizeRoutes(@RequestBody RouteRequestDTO request) {
        try {
            RouteSessionCreateRequestDTO sessionRequest = new RouteSessionCreateRequestDTO();
            sessionRequest.setSessionId(request.getSessionId());
            sessionRequest.setUserId(request.getUserId() != null ? request.getUserId() : DEFAULT_USER_ID);
            sessionRequest.setVehicleCount(request.getVehicleCount());
            sessionRequest.setVehicleCapacities(request.getVehicleCapacities());
            sessionRequest.setDepotLat(request.getDepotLat());
            sessionRequest.setDepotLng(request.getDepotLng());
            sessionRequest.setSelectedBinIds(request.getSelectedBinIds());

            RouteSessionSnapshotDTO snapshot = routeSessionService.optimizeAndBroadcast(sessionRequest);
            return ResponseEntity.ok(snapshot);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error optimizing routes: " + e.getMessage());
        }
    }
}