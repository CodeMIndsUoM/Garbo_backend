package com.garbo.api.controller;

import com.garbo.api.dto.websocket.LeaderboardUpdatePayload;
import com.garbo.core.service.LeaderboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/leaderboard")
@CrossOrigin(origins = "*")
public class LeaderboardController {

    @Autowired
    private LeaderboardService leaderboardService;

    @GetMapping("/top")
    public ResponseEntity<?> getTopLeaderboard(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String role
    ) {
        try {
            int safeLimit = Math.max(1, Math.min(limit, 100));
            leaderboardService.refreshLeaderboard();
            List<LeaderboardUpdatePayload.LeaderboardEntryDto> entries =
                    leaderboardService.getTopLeaderboard(safeLimit, role);

            Map<String, Object> data = new HashMap<>();
            data.put("entries", entries);
            data.put("updatedAt", System.currentTimeMillis());
            data.put("changedUser", null);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", data);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to load leaderboard", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to load leaderboard");
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserLeaderboardEntry(
            @PathVariable Long userId,
            @RequestParam(required = false) String role
    ) {
        try {
            leaderboardService.refreshLeaderboard();
            Optional<LeaderboardUpdatePayload.LeaderboardEntryDto> entry =
                    leaderboardService.getUserLeaderboardEntry(userId, role);

            Map<String, Object> response = new HashMap<>();
            if (entry.isEmpty()) {
                response.put("success", false);
                response.put("message", "Leaderboard entry not found");
                return ResponseEntity.status(404).body(response);
            }

            response.put("success", true);
            response.put("data", entry.get());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to load user leaderboard entry for userId={}", userId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Failed to load user leaderboard entry");
            return ResponseEntity.status(500).body(error);
        }
    }
}
