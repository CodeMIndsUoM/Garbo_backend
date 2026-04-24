package com.garbo.api.controller;

import com.garbo.infrastructure.config.security.CustomUserDetailsService;
import com.garbo.infrastructure.config.security.JwtUtil;
import com.garbo.core.service.UserService;
import com.garbo.core.entity.User;
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
    private final UserService userService;

    public AuthController(AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            CustomUserDetailsService userDetailsService,
            UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.userService = userService;
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

            // Prepare response (use Object values so booleans remain booleans)
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("role", role);
            response.put("email", email);

            // Fetch user entity to include mustChangePassword flag (do not block login)
            java.util.Optional<User> userOpt = userService.getByEmail(email);
            boolean mustChange = false;
            if (userOpt.isPresent()) {
                mustChange = userOpt.get().isMustChangePassword();
            }
            response.put("mustChangePassword", mustChange);

            return ResponseEntity.ok(response);

        } catch (AuthenticationException e) {
            // Invalid credentials
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid email or password");
            return ResponseEntity.status(401).body(error);
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");

        if (email == null || oldPassword == null || newPassword == null) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "email, oldPassword and newPassword are required");
            return ResponseEntity.badRequest().body(err);
        }

        try {
            userService.changePassword(email.trim(), oldPassword, newPassword);
            Map<String, String> resp = new HashMap<>();
            resp.put("message", "Password changed successfully");
            return ResponseEntity.ok(resp);
        } catch (java.util.NoSuchElementException ex) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "User not found");
            return ResponseEntity.status(404).body(err);
        } catch (IllegalArgumentException ex) {
            Map<String, String> err = new HashMap<>();
            err.put("error", "Invalid current password");
            return ResponseEntity.status(401).body(err);
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Token is valid");
        return ResponseEntity.ok(response);
    }
}
