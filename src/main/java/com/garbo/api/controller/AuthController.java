package com.garbo.api.controller;

import com.garbo.core.entity.Admin;
import com.garbo.core.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AdminService adminService;

    public AuthController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * Unified login endpoint using admins table only.
     * Determines user role based on emp_id:
     * - emp_id 1-100: SuperAdmin
     * - emp_id 101-500: Admin
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Email and password are required"
            ));
        }

        // Login using admins table
        Optional<Admin> admin = adminService.login(email, password);
        if (admin.isPresent()) {
            Long empId = admin.get().getEmpId();
            Map<String, Object> response = new HashMap<>();
            Map<String, Object> data = new HashMap<>();
            
            // Determine role based on emp_id
            if (empId >= 1 && empId <= 100) {
                // SuperAdmin (emp_id 1-100)
                response.put("success", true);
                response.put("role", "superadmin");
                data.put("empId", empId);
                data.put("email", admin.get().getEmail());
                data.put("council", admin.get().getCouncil());
                response.put("data", data);
                return ResponseEntity.ok(response);
            } else if (empId >= 101 && empId <= 500) {
                // Admin (emp_id 101-500)
                response.put("success", true);
                response.put("role", "admin");
                data.put("empId", empId);
                data.put("email", admin.get().getEmail());
                data.put("council", admin.get().getCouncil());
                response.put("data", data);
                return ResponseEntity.ok(response);
            }
        }

        // Invalid credentials or emp_id out of range
        return ResponseEntity.status(401).body(Map.of(
            "success", false,
            "message", "Invalid email or password"
        ));
    }
}
