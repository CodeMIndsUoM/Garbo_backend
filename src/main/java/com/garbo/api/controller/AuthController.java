package com.garbo.api.controller;

<<<<<<< HEAD
import com.garbo.core.entity.User;
import com.garbo.core.repository.UserRepository;
import com.garbo.infrastructure.config.security.CustomUserDetailsService;
import com.garbo.infrastructure.config.security.JwtUtil;
import com.garbo.core.service.UserService;
import com.garbo.core.entity.User;
import com.garbo.core.entity.AdminNew;
=======
import com.garbo.core.service.UserService;
import com.garbo.infrastructure.config.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
>>>>>>> kevin-RWS
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    @Autowired
    private UserService userService;

<<<<<<< HEAD
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;
    private final UserRepository userRepository;

    public AuthController(AuthenticationManager authenticationManager,
            JwtUtil jwtUtil,
            CustomUserDetailsService userDetailsService,
            UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
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
                    .map(auth -> auth.getAuthority())
                    .orElse("UNKNOWN");

            // Generate JWT
            String token = jwtUtil.generateToken(email, role);

            // Look up the user entity to get empId
            User user = userRepository.findFirstByEmailIgnoreCase(email)
                    .orElse(null);

            System.out.println("User lookup result: " + (user != null ? "found empId=" + user.getEmpId() + " name=" + user.getEmpName() : "NOT FOUND"));

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("role", role);
            response.put("email", email);
            if (user != null) {
                response.put("empId", user.getEmpId());
                response.put("empName", user.getEmpName());
            }

            // Minimal additive: include council info for admins; superadmins get null
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
=======
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
>>>>>>> kevin-RWS
    }
}
