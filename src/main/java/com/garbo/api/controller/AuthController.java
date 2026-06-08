package com.garbo.api.controller;

import com.garbo.infrastructure.config.security.CustomUserDetailsService;
import com.garbo.infrastructure.config.security.JwtUtil;
import com.garbo.core.service.UserService;
import com.garbo.core.entity.AdminNew;
import com.garbo.core.entity.BinCollector;
import com.garbo.core.entity.FieldMentor;
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

            // Fetch user entity to include mustChangePassword flag and user info
            java.util.Optional<User> userOpt = userService.getByEmail(email);
            boolean mustChange = false;
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                mustChange = user.isMustChangePassword();
                response.put("empId", user.getEmpId());
                response.put("empName", user.getEmpName());
                
                if (user instanceof BinCollector collector) {
                    response.put("onDuty", collector.isOnDuty());
                    response.put("rewardPoints", collector.getRewardPoints());
                }

                if (user instanceof FieldMentor mentor) {
                    response.put("onDuty", mentor.isOnDuty());
                    response.put("rewardPoints", mentor.getRewardPoints());
                }

                if (user instanceof com.garbo.core.entity.ThirdPartyCollector) {
                    com.garbo.core.entity.ThirdPartyCollector tpc =
                            (com.garbo.core.entity.ThirdPartyCollector) user;
                    if (tpc.getRegistrationStatus() != null) {
                        response.put("registrationStatus", tpc.getRegistrationStatus().name());
                    }
                    if (tpc.getAssignedCouncils() != null) {
                        response.put("assignedCouncils", tpc.getAssignedCouncils());
                    }
                }
            }
            response.put("mustChangePassword", mustChange);
            Object councilValue = null;
            if ("admin".equals(role)) {
                if (userOpt.isPresent() && userOpt.get() instanceof AdminNew) {
                    AdminNew admin = (AdminNew) userOpt.get();
                    String councilName = admin.getCouncil();
                    if (councilName != null) {
                        Map<String, String> councilMap = new HashMap<>();
                        councilMap.put("name", councilName);
                        councilValue = councilMap;
                    } else {
                        councilValue = null;
                    }
                } else {
                    councilValue = null;
                }
            } else {
                councilValue = null;
            }
            response.put("council", councilValue);

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
