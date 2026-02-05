package com.garbo.api.controller;

import com.garbo.core.entity.Admin;
import com.garbo.core.service.AdminService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class AdminController {

    final private AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping
    public ResponseEntity<?> createAdmin(@RequestBody Admin admin) {
        try {
            Admin saved = adminService.saveAdmin(admin);
            return ResponseEntity.ok().body(java.util.Map.of("success", true, "data", saved));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(java.util.Map.of("success", false, "message", "Failed to create admin"));
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> loginAdmin(@RequestBody java.util.Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");
        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", "Email and password required"));
        }
        java.util.Optional<Admin> adminOpt = adminService.login(email, password);
        if (adminOpt.isPresent()) {
            return ResponseEntity.ok().body(java.util.Map.of("success", true, "data", adminOpt.get()));
        } else {
            return ResponseEntity.status(401).body(java.util.Map.of("success", false, "message", "Invalid credentials"));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllAdmins() {
        try {
            return ResponseEntity.ok().body(java.util.Map.of("success", true, "data", adminService.getAllAdmins()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(java.util.Map.of("success", false, "message", "Failed to fetch admins"));
        }
    }

//    @GetMapping("/{empId}")
//    public Optional<Admin> getAdminById(@PathVariable Long empId) {
//        System.out.println("getting success");
//        System.out.println(adminService.getAdminById(empId));
//        return adminService.getAdminById(empId);
//    }
}
