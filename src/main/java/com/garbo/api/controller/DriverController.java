package com.garbo.api.controller;

import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.garbo.core.entity.Driver;
import com.garbo.core.service.DriverService;

import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/drivers")
@CrossOrigin(origins = "*")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @GetMapping
    public ResponseEntity<?> getAll(@RequestParam(required = false) String council) {
        if (council != null && !council.isBlank()) {
            return ResponseEntity.ok(Map.of("success", true, "data", driverService.getByCouncil(council)));
        }
        return ResponseEntity.ok(Map.of("success", true, "data", driverService.getAll()));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Driver payload) {
        System.out.println("DriverController: Received create request for " + payload.getName());
        try {
            Driver created = driverService.create(payload);
            System.out.println("DriverController: Successfully created driver with ID " + created.getId());
            return ResponseEntity.ok(Map.of("success", true, "data", created));
        } catch (IllegalArgumentException e) {
            System.out.println("DriverController: Validation error: " + e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            System.out.println("DriverController: Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", "Internal server error"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Driver payload) {
        try {
            return ResponseEntity.ok(Map.of("success", true, "data", driverService.update(id, payload)));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            driverService.delete(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Driver deleted"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
