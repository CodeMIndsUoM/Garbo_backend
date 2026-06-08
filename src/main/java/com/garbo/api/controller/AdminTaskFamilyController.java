package com.garbo.api.controller;

import com.garbo.core.service.TaskFamilyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admins/gamification/families")
public class AdminTaskFamilyController {

    private final TaskFamilyService taskFamilyService;

    public AdminTaskFamilyController(TaskFamilyService taskFamilyService) {
        this.taskFamilyService = taskFamilyService;
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(Map.of("success", true, "data", taskFamilyService.getAll()));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        try {
            var saved = taskFamilyService.create(
                    body.getOrDefault("code", ""),
                    body.getOrDefault("name", ""),
                    body.getOrDefault("description", ""));
            return ResponseEntity.ok(Map.of("success", true, "data", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return taskFamilyService.update(id, body.get("code"), body.get("name"), body.get("description"))
                .map(saved -> ResponseEntity.ok(Map.of("success", true, "data", saved)))
                .orElse(ResponseEntity.status(404).body(Map.of("success", false, "message", "Family not found")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!taskFamilyService.delete(id)) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Family not found"));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Deleted"));
    }
}
