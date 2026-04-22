package com.garbo.api.controller;

import com.garbo.core.service.UserService;
import com.garbo.infrastructure.config.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/validate")
    public ResponseEntity<?> validate(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.ok(Map.of("success", true, "data", false));
            }

            String token = authHeader.substring(7);
            String username = jwtUtil.extractUsername(token);
            boolean valid = jwtUtil.isTokenValid(token, username);

            return ResponseEntity.ok(Map.of("success", true, "data", valid));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", true, "data", false));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // Stateless JWT logout on client side.
        return ResponseEntity.ok(Map.of("success", true));
    }
}
