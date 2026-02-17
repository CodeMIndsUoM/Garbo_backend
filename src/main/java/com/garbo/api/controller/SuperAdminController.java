package com.garbo.api.controller;

import com.garbo.core.entity.SuperAdmin;
import com.garbo.core.service.SuperAdminService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/superadmins")
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    public SuperAdminController(SuperAdminService superAdminService) {
        this.superAdminService = superAdminService;
    }

    @PostMapping
    public ResponseEntity<?> createSuperAdmin(@RequestBody SuperAdmin superAdmin) {
        try {
            SuperAdmin saved = superAdminService.saveSuperAdmin(superAdmin);
            return ResponseEntity.ok().body(Map.of("success", true, "data", saved));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Failed to create superadmin"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginSuperAdmin(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");
        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Email and password required"));
        }
        Optional<SuperAdmin> superAdminOpt = superAdminService.login(email, password);
        if (superAdminOpt.isPresent()) {
            return ResponseEntity.ok().body(Map.of(
                    "success", true,
                    "data", superAdminOpt.get(),
                    "role", "superadmin"
            ));
        } else {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Invalid credentials"));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllSuperAdmins() {
        try {
            return ResponseEntity.ok().body(Map.of("success", true, "data", superAdminService.getAllSuperAdmins()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("success", false, "message", "Failed to fetch superadmins"));
        }
    }
}
