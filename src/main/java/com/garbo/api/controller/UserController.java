package com.garbo.api.controller;

import java.util.Map;
import java.util.Optional;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.garbo.core.entity.User;
import com.garbo.core.service.CollectorPerformanceService;
import com.garbo.core.service.UserGamificationTaskService;
import com.garbo.core.service.UserService;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private UserGamificationTaskService userGamificationTaskService;

    @Autowired
    private CollectorPerformanceService collectorPerformanceService;

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user) {
        try {
            User saved = userService.saveUser(user);
            return ResponseEntity.ok().body(Map.of("success", true, "data", saved));
        } catch (Exception e) {
            log.error("Failed to create user", e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Failed to create user"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> payload, HttpSession session) {
        String email = payload.get("email");
        String password = payload.get("password");
        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Email and password required"));
        }
        Optional<User> userOpt = userService.login(email, password);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Store userId in session for WebSocket handshake validation
            session.setAttribute("userId", user.getEmpId());
            session.setAttribute("userEmail", user.getEmail());
            log.info("User {} logged in. Session ID: {}", user.getEmpId(), session.getId());
            return ResponseEntity.ok().body(Map.of("success", true, "data", user));
        } else {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Invalid credentials"));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        try {
            return ResponseEntity.ok().body(Map.of("success", true, "data", userService.getAllUsers()));
        } catch (Exception e) {
            log.error("Failed to fetch users", e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Failed to fetch users"));
        }
    }

    @PutMapping("/{userId}")
    public ResponseEntity<?> updateUser(@PathVariable Long userId, @RequestBody User user) {
        try {
            Optional<User> updated = userService.updateUser(userId, user);
            if (updated.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("success", false, "message", "User not found"));
            }
            return ResponseEntity.ok().body(Map.of("success", true, "data", updated.get()));
        } catch (Exception e) {
            log.error("Failed to update user {}", userId, e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Failed to update user"));
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        try {
            boolean deleted = userService.deleteUser(userId);
            if (!deleted) {
                return ResponseEntity.status(404).body(Map.of("success", false, "message", "User not found"));
            }
            return ResponseEntity.ok().body(Map.of("success", true, "message", "User deleted"));
        } catch (Exception e) {
            log.error("Failed to delete user {}", userId, e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Failed to delete user"));
        }
    }

    @GetMapping("/{userId}/gamification-tasks")
    public ResponseEntity<?> getUserGamificationTasks(@PathVariable Long userId) {
        try {
            Optional<User> userOpt = userService.getById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("success", false, "message", "User not found"));
            }

            User user = userOpt.get();
            String role = user.getRole() != null ? user.getRole() : "COLLECTOR";

            return ResponseEntity.ok().body(Map.of(
                    "success", true,
                    "data", userGamificationTaskService.getUserTaskProgress(userId, role)
            ));
        } catch (Exception e) {
            log.error("Failed to fetch gamification tasks for user {}", userId, e);
            return ResponseEntity.status(500)
                    .body(Map.of("success", false, "message", "Failed to fetch gamification tasks"));
        }
    }

    @GetMapping("/{userId}/performance-stats")
    public ResponseEntity<?> getCollectorPerformanceStats(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok().body(Map.of(
                    "success", true,
                    "data", collectorPerformanceService.getCollectorPerformanceStats(userId)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Failed to fetch performance stats for user {}", userId, e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Failed to fetch performance stats"
            ));
        }
    }
}
