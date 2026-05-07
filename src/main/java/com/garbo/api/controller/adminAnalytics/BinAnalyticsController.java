package com.garbo.api.controller.adminAnalytics;



import com.garbo.core.service.AdminAnalytics.BinAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/bin-analytics")
@CrossOrigin("*")
public class BinAnalyticsController {

    @Autowired
    private BinAnalyticsService service;

    @GetMapping
    public ResponseEntity<?> getAnalytics() {
        try {
            return ResponseEntity.ok(service.getAnalytics());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}

