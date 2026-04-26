package com.garbo.api.controller;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.garbo.core.entity.User;
import com.garbo.core.service.UserService;
import com.garbo.infrastructure.storage.CloudinaryUploadService;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final CloudinaryUploadService cloudinaryUploadService;

    public UserController(UserService userService, CloudinaryUploadService cloudinaryUploadService) {
        this.userService = userService;
        this.cloudinaryUploadService = cloudinaryUploadService;
    }

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
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");
        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Email and password required"));
        }
        Optional<User> userOpt = userService.login(email, password);
        if (userOpt.isPresent()) {
            return ResponseEntity.ok().body(Map.of("success", true, "data", userOpt.get()));
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

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        Optional<User> userOpt = userService.getUserById(id);
        if (userOpt.isPresent()) {
            return ResponseEntity.ok().body(Map.of("success", true, "data", userOpt.get()));
        } else {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "User not found"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User updatedDetails) {
        try {
            User saved = userService.updateUser(id, updatedDetails);
            return ResponseEntity.ok().body(Map.of("success", true, "data", saved));
        } catch (Exception e) {
            log.error("Failed to update user", e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Failed to update user"));
        }
    }

    @PostMapping("/{id}/avatar")
    public ResponseEntity<?> uploadAvatar(@PathVariable Long id, @RequestParam("photo") MultipartFile photo) {
        try {
            // First verify user exists
            Optional<User> userOpt = userService.getUserById(id);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("success", false, "message", "User not found"));
            }
            
            // Upload photo using Cloudinary
            String avatarUrl = cloudinaryUploadService.uploadProfilePhoto(photo, id);

            // Update user with new avatar
            User userToUpdate = new User();
            userToUpdate.setAvatarUrl(avatarUrl);
            User updated = userService.updateUser(id, userToUpdate);

            return ResponseEntity.ok().body(Map.of("success", true, "data", updated));
        } catch (Exception e) {
            log.error("Failed to upload avatar", e);
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Failed to upload avatar"));
        }
    }
}
