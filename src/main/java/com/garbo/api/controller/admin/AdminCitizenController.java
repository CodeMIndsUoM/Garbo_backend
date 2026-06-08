package com.garbo.api.controller.admin;

import com.garbo.api.dto.common.ApiResponse;
import com.garbo.core.entity.Citizen;
import com.garbo.core.service.AdminCitizenService;
import com.garbo.infrastructure.config.security.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin/citizens")
@CrossOrigin(origins = "*")
public class AdminCitizenController {

    private final AdminCitizenService adminCitizenService;
    private final JwtUtil jwtUtil;

    public AdminCitizenController(AdminCitizenService adminCitizenService, JwtUtil jwtUtil) {
        this.adminCitizenService = adminCitizenService;
        this.jwtUtil = jwtUtil;
    }

    private String resolveRequesterEmail(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null && !"anonymousUser".equalsIgnoreCase(auth.getName())) {
            return auth.getName();
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                return jwtUtil.extractUsername(authHeader.substring(7));
            } catch (Exception ignored) {
            }
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid bearer token");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Citizen>>> listCitizens(
            @RequestParam(required = false) String council,
            HttpServletRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.success(
                    adminCitizenService.listCitizens(resolveRequesterEmail(request), council)));
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            return ResponseEntity.status(403).body(ApiResponse.error(ex.getMessage(), "FORBIDDEN"));
        }
    }
}
