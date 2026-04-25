package com.garbo.api.controller.adminAnalytics;



import com.garbo.api.dto.ThirdPartyAnalyseDTOs.ThirdPartyAnalyticsResponseDTO;
import com.garbo.core.service.AdminAnalytics.ThirdPartyAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class ThirdPartyAnalyticsController {

    private final ThirdPartyAnalyticsService service;

    /**
     * GET /api/admin/thirdparty/analyze?period=TODAY
     * GET /api/admin/thirdparty/analyze?period=LAST_WEEK
     * GET /api/admin/thirdparty/analyze?period=LAST_MONTH
     * GET /api/admin/thirdparty/analyze          (defaults to ALL)
     */
    @GetMapping("/thirdparty/analyze")
    public ResponseEntity<?> getThirdPartyAnalytics(
            @RequestParam(name = "period", required = false, defaultValue = "ALL") String period
    ) {
        try {
            ThirdPartyAnalyticsResponseDTO response = service.getAnalytics(period);
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