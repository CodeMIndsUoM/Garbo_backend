package com.garbo.api.controller;

import com.garbo.api.dto.websocket.LeaderboardUpdatePayload;
import com.garbo.core.service.LeaderboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
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
}
