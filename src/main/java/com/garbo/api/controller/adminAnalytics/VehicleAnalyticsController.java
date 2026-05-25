package com.garbo.api.controller.adminAnalytics;

import com.garbo.api.dto.VehicleAnalyticsDTOs.VehicleAnalyticsDTO;
import com.garbo.core.service.AdminAnalytics.VehicleAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/vehicles")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VehicleAnalyticsController {

    private final VehicleAnalyticsService vehicleAnalyticsService;

    @GetMapping("/analytics")
    public ResponseEntity<?> getAnalytics(
        @RequestParam(value = "councilId", required = false) String councilId
    ) {
        try {
            return ResponseEntity.ok(vehicleAnalyticsService.getAnalytics(councilId));
        } catch (Exception e) {
            log.error("Failed to fetch vehicle analytics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error",   e.getClass().getSimpleName(),
                "message", e.getMessage() != null ? e.getMessage() : "No message",
                "cause",   e.getCause() != null ? e.getCause().getMessage() : "No cause"
            ));
        }
    }

    @GetMapping("/analytics/filter")
    public ResponseEntity<?> getAnalyticsByStatus(
        @RequestParam(value = "status",    defaultValue = "all") String status,
        @RequestParam(value = "councilId", required = false)     String councilId
    ) {
        try {
            return ResponseEntity.ok(vehicleAnalyticsService.getAnalyticsByStatus(status, councilId));
        } catch (Exception e) {
            log.error("Failed to fetch filtered vehicle analytics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error",   e.getClass().getSimpleName(),
                "message", e.getMessage() != null ? e.getMessage() : "No message",
                "cause",   e.getCause() != null ? e.getCause().getMessage() : "No cause"
            ));
        }
    }
}