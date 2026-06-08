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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

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

    @PostMapping("/{empId}/hide")
    public ResponseEntity<ApiResponse<Map<String, Object>>> hideCitizen(
            @PathVariable Long empId,
            HttpServletRequest request) {
        String result = adminCitizenService.hideCitizen(resolveRequesterEmail(request), empId);
        return switch (result) {
            case "HIDDEN" -> ResponseEntity.ok(ApiResponse.success(Map.of(
                    "empId", empId,
                    "message", "Citizen hidden from admin list")));
            case "NOT_FOUND" -> ResponseEntity.status(404)
                    .body(ApiResponse.error("Citizen not found", "NOT_FOUND"));
            case "FORBIDDEN" -> ResponseEntity.status(403)
                    .body(ApiResponse.error("Not allowed to manage this citizen", "FORBIDDEN"));
            default -> ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to hide citizen", "ERROR"));
        };
    }

    @DeleteMapping("/{empId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteCitizen(
            @PathVariable Long empId,
            HttpServletRequest request) {
        String result = adminCitizenService.deleteCitizen(resolveRequesterEmail(request), empId);
        return switch (result) {
            case "DELETED" -> ResponseEntity.ok(ApiResponse.success(Map.of(
                    "empId", empId,
                    "message", "Citizen deleted")));
            case "NOT_FOUND" -> ResponseEntity.status(404)
                    .body(ApiResponse.error("Citizen not found", "NOT_FOUND"));
            case "FORBIDDEN" -> ResponseEntity.status(403)
                    .body(ApiResponse.error("Not allowed to manage this citizen", "FORBIDDEN"));
            case "CONFLICT" -> ResponseEntity.status(409)
                    .body(ApiResponse.error("Cannot delete due to linked records", "CONFLICT"));
            default -> ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to delete citizen", "ERROR"));
        };
    }
}
