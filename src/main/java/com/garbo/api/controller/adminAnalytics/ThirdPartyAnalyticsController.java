package com.garbo.api.controller.adminAnalytics;

import com.garbo.api.dto.ThirdPartyAnalyseDTOs.ThirdPartyAnalyticsResponseDTO;
import com.garbo.core.service.AdminAnalytics.ThirdPartyAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class ThirdPartyAnalyticsController {

    private final ThirdPartyAnalyticsService service;

    @GetMapping("/thirdparty/analyze")
    public ResponseEntity<?> getThirdPartyAnalytics(
            @RequestParam(name = "period",    required = false, defaultValue = "ALL") String period,
            @RequestParam(name = "councilId", required = false)                       String councilId
    ) {
        try {
            ThirdPartyAnalyticsResponseDTO response = service.getAnalytics(period, councilId);
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