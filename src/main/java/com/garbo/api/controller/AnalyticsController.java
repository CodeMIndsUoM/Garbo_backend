package com.garbo.api.controller;

import com.garbo.api.dto.Collect_analyze_dtos.DashboardResponseDTO;
import com.garbo.core.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/analytics")
@CrossOrigin("*")
public class AnalyticsController {

    @Autowired
    private AnalyticsService service;

    @GetMapping
    public ResponseEntity<?> getDashboard(
            @RequestParam(defaultValue = "DAY") String filter) {
        try {
            DashboardResponseDTO result = service.getDashboard(filter.toUpperCase());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            // Full error chain printed to console
            e.printStackTrace();
            // Exact error message returned to Postman
            return ResponseEntity.status(500).body(
                "ERROR: " + e.getMessage() +
                "\nCAUSE: " + (e.getCause() != null ? e.getCause().getMessage() : "null") +
                "\nROOT: " + (e.getCause() != null && e.getCause().getCause() != null ? e.getCause().getCause().getMessage() : "null")
            );
        }
    }
}