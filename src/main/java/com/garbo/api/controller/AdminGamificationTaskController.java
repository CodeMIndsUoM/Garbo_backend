package com.garbo.api.controller;

import com.garbo.api.dto.gamification.AdminGamificationTaskUpsertRequest;
import com.garbo.api.dto.gamification.TaskScoreAwardRequest;
import com.garbo.core.entity.GamificationTask;
import com.garbo.core.service.GamificationTaskService;
import com.garbo.core.service.UserTaskProgressService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admins/gamification/tasks")
public class AdminGamificationTaskController {

    @Autowired
    private GamificationTaskService gamificationTaskService;

    @Autowired
    private UserTaskProgressService userTaskProgressService;

    @GetMapping
    public ResponseEntity<?> getAllTasks() {
        return ResponseEntity.ok(Map.of("success", true, "data", gamificationTaskService.getAllTasks()));
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<?> getTask(@PathVariable Long taskId) {
        Optional<GamificationTask> taskOpt = gamificationTaskService.getTaskById(taskId);
        if (taskOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Task not found"));
        }
        return ResponseEntity.ok(Map.of("success", true, "data", taskOpt.get()));
    }

    @PostMapping
    public ResponseEntity<?> createTask(@RequestBody AdminGamificationTaskUpsertRequest request) {
        try {
            GamificationTask saved = gamificationTaskService.createTask(request);
            return ResponseEntity.ok(Map.of("success", true, "data", saved));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Failed to create task"));
        }
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<?> updateTask(@PathVariable Long taskId, @RequestBody AdminGamificationTaskUpsertRequest request) {
        Optional<GamificationTask> taskOpt = gamificationTaskService.updateTask(taskId, request);
        if (taskOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Task not found"));
        }
        return ResponseEntity.ok(Map.of("success", true, "data", taskOpt.get()));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<?> deleteTask(@PathVariable Long taskId) {
        boolean deleted = gamificationTaskService.deleteTask(taskId);
        if (!deleted) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Task not found"));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Task archived"));
    }

    @PostMapping("/{taskId}/publish")
    public ResponseEntity<?> publishTask(@PathVariable Long taskId, @RequestParam(required = false) Long adminId) {
        Optional<GamificationTask> taskOpt = gamificationTaskService.updateTaskStatus(taskId, "PUBLISHED", adminId);
        if (taskOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Task not found"));
        }
        return ResponseEntity.ok(Map.of("success", true, "data", taskOpt.get()));
    }

    @PostMapping("/{taskId}/pause")
    public ResponseEntity<?> pauseTask(@PathVariable Long taskId, @RequestParam(required = false) Long adminId) {
        Optional<GamificationTask> taskOpt = gamificationTaskService.updateTaskStatus(taskId, "PAUSED", adminId);
        if (taskOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Task not found"));
        }
        return ResponseEntity.ok(Map.of("success", true, "data", taskOpt.get()));
    }

    @PostMapping("/{taskId}/archive")
    public ResponseEntity<?> archiveTask(@PathVariable Long taskId, @RequestParam(required = false) Long adminId) {
        Optional<GamificationTask> taskOpt = gamificationTaskService.updateTaskStatus(taskId, "ARCHIVED", adminId);
        if (taskOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Task not found"));
        }
        return ResponseEntity.ok(Map.of("success", true, "data", taskOpt.get()));
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActiveTasks(@RequestParam String role) {
        return ResponseEntity.ok(Map.of("success", true, "data", gamificationTaskService.getActiveTasksForRole(role)));
    }

    @PostMapping("/award")
    public ResponseEntity<?> awardPointsForTaskCompletion(@RequestBody TaskScoreAwardRequest request) {
        try {
                return userTaskProgressService.incrementTaskProgress(
                request.getUserId(),
                request.getRole(),
                request.getTaskId(),
                request.getSourceEventId(),
                request.getReason(),
                request.getPriorityLevel(),
                request.getProgressIncrement()
                ).<ResponseEntity<?>>map(progress -> ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Progress processed"
                ))).orElseGet(() -> ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Invalid task progress request"
            )));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Failed to process progress"));
        }
    }
}
