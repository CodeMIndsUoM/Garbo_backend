package com.garbo.api.controller;

import com.garbo.api.dto.websocket.LeaderboardUpdatePayload;
import com.garbo.core.service.LeaderboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leaderboard")
@CrossOrigin(origins = "*")
public class LeaderboardController {

    @Autowired
    private LeaderboardService leaderboardService;

    @GetMapping("/top")
    public ResponseEntity<?> getTopLeaderboard(@RequestParam(defaultValue = "10") int limit) {
        try {
            int safeLimit = Math.max(1, Math.min(limit, 100));
            List<LeaderboardUpdatePayload.LeaderboardEntryDto> entries = leaderboardService.getTopLeaderboard(safeLimit);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", Map.of(
                            "entries", entries,
                            "updatedAt", System.currentTimeMillis(),
                            "changedUser", null
                    )
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Failed to load leaderboard"
            ));
        }
    }
}
