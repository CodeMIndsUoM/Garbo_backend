package com.garbo.api.controller.adminAnalytics;

import com.garbo.api.dto.binReportAnalyticsDTOs.BinReportAnalyticsDTO;
import com.garbo.core.service.AdminAnalytics.BinReportAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/bin-reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BinReportAnalyticsController {

    private final BinReportAnalyticsService analyticsService;

    @GetMapping("/analytics")
    public ResponseEntity<?> getAnalytics(
            @RequestParam(required = false) String council) {
        try {
            BinReportAnalyticsDTO result = analyticsService.getAnalytics(council);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Failed to fetch bin report analytics", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error",   e.getClass().getSimpleName(),
                "message", e.getMessage() != null ? e.getMessage() : "No message",
                "cause",   e.getCause() != null ? e.getCause().getMessage() : "No cause"
            ));
        }
    }
}