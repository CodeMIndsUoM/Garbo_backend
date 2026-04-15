package com.garbo.api.controller;

import com.garbo.core.entity.Vehicle;
import com.garbo.core.exception.DuplicateResourceException;
import com.garbo.core.exception.ResourceNotFoundException;
import com.garbo.core.service.VehicleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicles")
@CrossOrigin(origins = "*")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    public ResponseEntity<?> createVehicle(@RequestBody Vehicle vehicle) {
        try {
            Vehicle saved = vehicleService.createVehicle(vehicle);
            return ResponseEntity.status(201).body(Map.of("success", true, "data", saved));
        } catch (DuplicateResourceException e) {
            return ResponseEntity.status(409).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Failed to create vehicle"));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllVehicles(@RequestParam(required = false) String council) {
        try {
            List<Vehicle> vehicles;
            if (council != null && !council.isEmpty()) {
                vehicles = vehicleService.getVehiclesByCouncil(council);
            } else {
                vehicles = vehicleService.getAllVehicles();
            }
            return ResponseEntity.ok(Map.of("success", true, "data", vehicles));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Failed to fetch vehicles"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getVehicleById(@PathVariable Long id) {
        try {
            return vehicleService.getVehicleById(id)
                    .map(vehicle -> ResponseEntity.ok(Map.of("success", true, "data", vehicle)))
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Failed to fetch vehicle"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateVehicle(@PathVariable Long id, @RequestBody Vehicle vehicle) {
        try {
            Vehicle updated = vehicleService.updateVehicle(id, vehicle);
            return ResponseEntity.ok(Map.of("success", true, "data", updated));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
        } catch (DuplicateResourceException e) {
            return ResponseEntity.status(409).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Failed to update vehicle"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVehicle(@PathVariable Long id) {
        try {
            vehicleService.deleteVehicle(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Vehicle deleted successfully"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Failed to delete vehicle"));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateVehicleStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        try {
            String status = payload.get("status");
            if (status == null || status.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Status is required"));
            }
            Vehicle updated = vehicleService.updateVehicleStatus(id, status);
            return ResponseEntity.ok(Map.of("success", true, "data", updated));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Failed to update vehicle status"));
        }
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<?> assignDriver(@PathVariable Long id, @RequestBody Map<String, Long> payload) {
        try {
            Long driverId = payload.get("driverId");
            if (driverId == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Driver ID is required"));
            }
            Vehicle updated = vehicleService.assignDriver(id, driverId);
            return ResponseEntity.ok(Map.of("success", true, "data", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Failed to assign driver"));
        }
    }
}
