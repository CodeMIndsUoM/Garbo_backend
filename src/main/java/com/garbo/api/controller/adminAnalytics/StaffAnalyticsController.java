package com.garbo.api.controller.adminAnalytics;


import com.garbo.api.dto.staffAnalyzeDTOs.StaffAnalyticsResponseDTO;
import com.garbo.core.service.AdminAnalytics.StaffAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin("*")
@RequiredArgsConstructor
public class StaffAnalyticsController {

    private final StaffAnalyticsService service;

    @GetMapping("/staffanalytics")
    public ResponseEntity<?> getStaffAnalytics(
            @RequestParam(required = false) String councilId) {
        try {
            StaffAnalyticsResponseDTO response = service.getAnalytics(councilId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(
                "ERROR: " + e.getMessage() +
                "\nCAUSE: " + (e.getCause() != null ? e.getCause().getMessage() : "null")
            );
        }
    }
}