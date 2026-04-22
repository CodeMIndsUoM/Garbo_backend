package com.garbo.api.controller;

import com.garbo.infrastructure.config.security.CustomUserDetailsService;
import com.garbo.infrastructure.config.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    public AuthController(AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            CustomUserDetailsService userDetailsService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");

        System.out.println("AuthController.login called for email=" + email);
        try {
            java.nio.file.Files.writeString(java.nio.file.Paths.get("auth-attempt.log"),
                    java.time.Instant.now() + " - " + email + System.lineSeparator(),
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ex) {
            System.out.println("Failed to write auth log: " + ex.getMessage());
        }

        try {
            // Authenticate with Spring Security
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password));

            System.out.println("AuthenticationManager.authenticate succeeded for " + email);

            // Load full user entity via CustomUserDetailsService
            var userDetails = userDetailsService.loadUserByUsername(email);
            // Make sure your CustomUserDetailsService sets the role properly
            String role = userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(auth -> auth.getAuthority().replace("ROLE_", "").trim().toLowerCase())
                    .orElse("unknown");
            // Generate JWT
            String token = jwtUtil.generateToken(email, role);

            // Prepare response
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("role", role);
            response.put("email", email);

            return ResponseEntity.ok(response);

        } catch (AuthenticationException e) {
            // Invalid credentials
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid email or password");
            return ResponseEntity.status(401).body(error);
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Token is valid");
        return ResponseEntity.ok(response);
    }
}
