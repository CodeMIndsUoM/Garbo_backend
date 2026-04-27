package com.garbo.api.controller.adminAnalytics;


import com.garbo.api.dto.ComplaintDTOs.ComplaintAnalyticsResponseDTO;
import com.garbo.core.service.AdminAnalytics.ComplaintAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * GET /api/admin/complaintanalytics?filter=TODAY|WEEK|MONTH
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin("*")
@RequiredArgsConstructor
public class ComplaintAnalyticsController {

    private final ComplaintAnalyticsService service;

    @GetMapping("/complaintanalytics")
    public ResponseEntity<?> getComplaintAnalytics(
            @RequestParam(defaultValue = "TODAY") String filter) {
        try {
            ComplaintAnalyticsResponseDTO response = service.getAnalytics(filter);
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
