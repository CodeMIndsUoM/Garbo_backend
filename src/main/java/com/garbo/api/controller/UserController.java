package com.garbo.api.controller;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.garbo.core.entity.User;
import com.garbo.core.entity.AdminNew;
import com.garbo.core.service.UserService;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody Map<String, Object> payload) {
        try {
            // If payload contains 'council' treat as new Admin creation
            if (payload.containsKey("council")) {
                AdminNew admin = new AdminNew();
                // map frontend fields
                Object fullName = payload.get("fullName");
                Object email = payload.get("email");
                Object contactNumber = payload.get("contactNumber");
                Object council = payload.get("council");

                if (fullName != null)
                    admin.setEmpName(fullName.toString());
                if (email != null)
                    admin.setEmail(email.toString());
                if (contactNumber != null)
                    admin.setPhone(contactNumber.toString());
                if (council != null)
                    admin.setCouncil(council.toString());

                admin.setRole("ADMIN");
                admin.setCreatedAt(LocalDateTime.now());

                AdminNew saved = userService.saveAdminNew(admin);
                return ResponseEntity.ok().body(Map.of("success", true, "data", saved));
            }

            // Fallback: convert payload to User (supports existing clients)
            User user = MAPPER.convertValue(payload, User.class);
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
}
