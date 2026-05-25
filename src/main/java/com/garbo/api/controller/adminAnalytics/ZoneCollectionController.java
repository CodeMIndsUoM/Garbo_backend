package com.garbo.api.controller.adminAnalytics;


import com.garbo.api.dto.ZoneCollectionDTO;
import com.garbo.core.service.AdminAnalytics.ZoneCollectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ZoneCollectionController {

    private final ZoneCollectionService service;

    @GetMapping("/zone-collection")
    public ResponseEntity<?> getZoneCollection(
            @RequestParam(defaultValue = "DAILY") String filter,
            @RequestParam(required = false) String council) {
        try {
            List<ZoneCollectionDTO> result = service.getZoneCollection(filter, council);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to fetch zone collection data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error",   e.getClass().getSimpleName(),
                "message", e.getMessage() != null ? e.getMessage() : "No message",
                "cause",   e.getCause() != null ? e.getCause().getMessage() : "No cause"
            ));
        }
    }
}