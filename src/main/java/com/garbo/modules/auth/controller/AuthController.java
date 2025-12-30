package com.garbo.modules.auth.controller;

import com.garbo.modules.auth.admin.model.Admin;
import com.garbo.modules.auth.admin.repository.AdminRepository;
import com.garbo.modules.auth.superAdmin.model.SuperAdmin;
import com.garbo.modules.auth.superAdmin.repository.SuperAdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000")
public class AuthController {
    @Autowired
    private AdminRepository adminRepository;
    @Autowired
    private SuperAdminRepository superAdminRepository;

    @GetMapping("/login")
    public ResponseEntity<?> login(@RequestParam String username, @RequestParam String password) {
        Optional<Admin> adminOpt = adminRepository.findByEmailAndPassword(username, password);
        if (adminOpt.isPresent()) {
            Admin admin = adminOpt.get();
            return ResponseEntity.ok(Map.of(
                "role", "admin",
                "email", admin.getEmail()
            ));
        }
        Optional<SuperAdmin> superAdminOpt = superAdminRepository.findByEmailAndPassword(username, password);
        if (superAdminOpt.isPresent()) {
            SuperAdmin superAdmin = superAdminOpt.get();
            return ResponseEntity.ok(Map.of(
                "role", "superadmin",
                "email", superAdmin.getEmail()
            ));
        }
        return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        // For now, just return success if any token is present
        if (authHeader != null && !authHeader.isEmpty()) {
            return ResponseEntity.ok(Map.of("success", true, "data", true));
        } else {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "No token provided"));
        }
    }
}
