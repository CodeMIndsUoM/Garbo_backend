package com.garbo.api.controller;

import com.garbo.api.dto.websocket.TaskProgressUpdatePayload;
import com.garbo.api.dto.websocket.WebSocketMessage;
import com.garbo.core.entity.UserTaskProgress;
import com.garbo.core.service.BinCollectionRealtimeService;
import com.garbo.core.service.CollectorPerformanceService;
import com.garbo.core.service.UserTaskProgressService;
import com.garbo.infrastructure.websocket.WebSocketSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bincollectors")
public class BinCollectorController {
    @Autowired
    private BinCollectionRealtimeService binCollectionRealtimeService;

    @Autowired
    private CollectorPerformanceService collectorPerformanceService;

    @Autowired
    private UserTaskProgressService userTaskProgressService;

    @Autowired
    private com.garbo.core.service.route.RouteAssignmentService routeAssignmentService;

    @Autowired
    private WebSocketSessionManager webSocketSessionManager;
    
    /**
     * Record a bin collection by a collector.
     * Awards points and broadcasts leaderboard update.
     */
    @PostMapping("/{collectorId}/collect-bin")
    public ResponseEntity<?> collectBin(
            @PathVariable Long collectorId,
            @RequestBody Map<String, Object> request) {
        try {
            Long binId = ((Number) request.get("binId")).longValue();
            String priority = (String) request.getOrDefault("priority", "MEDIUM");
            double basePoints = ((Number) request.getOrDefault("basePoints", 10.0)).doubleValue();

                var result = binCollectionRealtimeService.processBinCollected(
                    collectorId,
                    binId,
                    priority,
                    basePoints,
                    null
                );
            
            return ResponseEntity.ok().body(Map.of(
                    "success", true,
                    "message", "Bin collected and realtime progress updated",
                    "collectorId", result.userId(),
                    "binId", result.binId(),
                    "totalBinsCollected", result.totalBinsCollected(),
                    "affectedTasks", result.affectedTasks()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Failed to process bin collection: " + e.getMessage()
            ));
        }
    }

        @PostMapping("/{collectorId}/route-completion")
        public ResponseEntity<?> reportRouteCompletion(
            @PathVariable Long collectorId,
            @RequestBody Map<String, Object> request
        ) {
        try {
            String sessionId = request.get("sessionId") != null
                ? request.get("sessionId").toString()
                : null;
            Integer assignedBins = request.get("assignedBins") instanceof Number n
                ? n.intValue()
                : null;
            Integer collectedBins = request.get("collectedBins") instanceof Number n
                ? n.intValue()
                : null;
            Integer missedBins = request.get("missedBins") instanceof Number n
                ? n.intValue()
                : null;
            Long durationSeconds = request.get("durationSeconds") instanceof Number n
                ? n.longValue()
                : null;
            LocalDateTime completedAt = null;
            if (request.get("completedAt") != null) {
            completedAt = LocalDateTime.parse(request.get("completedAt").toString());
            }

            var completion = collectorPerformanceService.recordRouteCompletion(
                collectorId,
                sessionId,
                assignedBins,
                collectedBins,
                missedBins,
                durationSeconds,
                completedAt
            );

                List<UserTaskProgress> routeTaskUpdates = userTaskProgressService.incrementDailyRouteTasksForCompletion(
                    collectorId,
                    "COLLECTOR",
                    sessionId
                );
                
                if (sessionId != null) {
                    try {
                        routeAssignmentService.completeRouteSession(java.util.UUID.fromString(sessionId));
                    } catch (Exception e) {
                        System.err.println("Failed to update route assignment completion status: " + e.getMessage());
                    }
                }
                
                broadcastTaskProgressUpdate(
                    collectorId,
                    collectedBins != null ? collectedBins : 0,
                    routeTaskUpdates
                );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Route completion recorded",
                "data", completion
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Failed to record route completion: " + e.getMessage()
            ));
        }
        }

        private void broadcastTaskProgressUpdate(
            Long userId,
            int totalBinsCollected,
            List<UserTaskProgress> updatedTasks
        ) {
        List<TaskProgressUpdatePayload.TaskProgressItem> taskItems = new ArrayList<>();
        for (UserTaskProgress progress : updatedTasks) {
            taskItems.add(new TaskProgressUpdatePayload.TaskProgressItem(
                progress.getTask().getId(),
                progress.getTask().getCode(),
                progress.getTask().getTitle(),
                progress.getTask().getDescription(),
                progress.getTask().getBasePoints(),
                progress.getCurrentProgress(),
                progress.getTargetProgress(),
                progress.isCompleted(),
                progress.getCompletedAt() != null
                    ? progress.getCompletedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    : null,
                progress.getPointsEarned()
            ));
        }

        TaskProgressUpdatePayload payload = new TaskProgressUpdatePayload(
            userId,
            null,
            totalBinsCollected,
            System.currentTimeMillis(),
            taskItems
        );

        webSocketSessionManager.sendToUser(
            userId,
            new WebSocketMessage<>("TASK_PROGRESS_UPDATE", userId, payload)
        );
        }
}
