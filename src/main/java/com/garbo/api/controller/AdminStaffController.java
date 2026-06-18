package com.garbo.api.controller;

import com.garbo.api.dto.staff.StaffCreateRequest;

import com.garbo.api.dto.staff.StaffListDto;
import com.garbo.api.dto.staff.StaffUpdateRequest;
import com.garbo.core.service.AdminStaffService;
import com.garbo.core.service.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admins/staff")
@CrossOrigin(origins = "*")
public class AdminStaffController {

    private final AdminStaffService adminStaffService;

    public AdminStaffController(AdminStaffService adminStaffService) {
        this.adminStaffService = adminStaffService;
    }

    @PostMapping("/field-mentors")
    public ResponseEntity<?> createFieldMentor(@RequestBody StaffCreateRequest req) {
        String role = CurrentUserService.getCurrentRole().orElse("");
        if (!"admin".equals(role) && !"superadmin".equals(role)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Forbidden"));
        }

        String council = resolveCreateCouncil(role, req);
        if (council == null) {
            return ResponseEntity.status(400)
                    .body(Map.of("success", false, "message",
                            "superadmin".equals(role)
                                    ? "Council is required when creating staff"
                                    : "Admin has no council assigned"));
        }

        var createdOpt = adminStaffService.createFieldMentor(req, council);
        if (createdOpt.isEmpty()) {
            return ResponseEntity.status(400).body(Map.of("success", false, "message",
                    "Failed to create field mentor (email may exist or payload invalid)"));
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("data", createdOpt.get());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/bin-collectors")
    public ResponseEntity<?> createBinCollector(@RequestBody StaffCreateRequest req) {
        String role = CurrentUserService.getCurrentRole().orElse("");
        if (!"admin".equals(role) && !"superadmin".equals(role)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Forbidden"));
        }

        String council = resolveCreateCouncil(role, req);
        if (council == null) {
            return ResponseEntity.status(400)
                    .body(Map.of("success", false, "message",
                            "superadmin".equals(role)
                                    ? "Council is required when creating staff"
                                    : "Admin has no council assigned"));
        }

        var createdOpt = adminStaffService.createBinCollector(req, council);
        if (createdOpt.isEmpty()) {
            return ResponseEntity.status(400).body(Map.of("success", false, "message",
                    "Failed to create bin collector (email may exist or payload invalid)"));
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("data", createdOpt.get());
        return ResponseEntity.ok(resp);
    }

    @GetMapping
    public ResponseEntity<?> listStaff(@RequestParam(required = false) String council) {
        String role = CurrentUserService.getCurrentRole().orElse("");
        // allow admin to list own-council staff, and allow superadmin to list all
        // (read-only)
        if (!"admin".equals(role) && !"superadmin".equals(role)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Forbidden"));
        }

        String currentCouncil = CurrentUserService.getCurrentCouncil().orElse(null);
        if ("admin".equals(role) && currentCouncil == null) {
            return ResponseEntity.status(400)
                    .body(Map.of("success", false, "message", "Admin has no council assigned"));
        }

        // Admin must always use their own council; superadmin may optionally filter via
        // query param
        String effectiveCouncil = "admin".equals(role) ? currentCouncil : council;

        List<StaffListDto> list = adminStaffService.listStaffForCurrentAdmin(effectiveCouncil);
        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("data", list);
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/field-mentors/{empId}")
    public ResponseEntity<?> updateFieldMentor(
            @PathVariable Long empId,
            @RequestBody StaffUpdateRequest req) {
        return updateStaffResponse(true, empId, req);
    }

    @PutMapping("/bin-collectors/{empId}")
    public ResponseEntity<?> updateBinCollector(
            @PathVariable Long empId,
            @RequestBody StaffUpdateRequest req) {
        return updateStaffResponse(false, empId, req);
    }

    private ResponseEntity<?> updateStaffResponse(boolean mentor, Long empId, StaffUpdateRequest req) {
        String role = CurrentUserService.getCurrentRole().orElse("");
        if (!"admin".equals(role) && !"superadmin".equals(role)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Forbidden"));
        }
        String council = resolveManageCouncil(role);
        if ("admin".equals(role) && council == null) {
            return ResponseEntity.status(400)
                    .body(Map.of("success", false, "message", "Admin has no council assigned"));
        }
        var updatedOpt = mentor
                ? adminStaffService.updateFieldMentor(empId, req, council)
                : adminStaffService.updateBinCollector(empId, req, council);
        if (updatedOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Staff member not found"));
        }
        return ResponseEntity.ok(Map.of("success", true, "data", updatedOpt.get()));
    }

    private String resolveCreateCouncil(String role, StaffCreateRequest req) {
        if ("superadmin".equals(role)) {
            if (req == null || req.getCouncil() == null || req.getCouncil().isBlank()) {
                return null;
            }
            return req.getCouncil().trim();
        }
        return CurrentUserService.getCurrentCouncil().orElse(null);
    }

    @PostMapping("/{id}/hide")
    public ResponseEntity<?> hideStaff(@PathVariable Long id) {
        String role = CurrentUserService.getCurrentRole().orElse("");
        if (!"admin".equals(role) && !"superadmin".equals(role)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Forbidden"));
        }

        String council = resolveManageCouncil(role);
        if ("admin".equals(role) && council == null) {
            return ResponseEntity.status(400)
                    .body(Map.of("success", false, "message", "Admin has no council assigned"));
        }

        String result = adminStaffService.hideInternalUser(id, council);
        return switch (result) {
            case "HIDDEN" -> ResponseEntity.ok(Map.of("success", true, "message", "Hidden"));
            case "NOT_FOUND" -> ResponseEntity.status(404).body(Map.of("success", false, "message", "User not found"));
            case "NOT_INTERNAL" ->
                ResponseEntity.status(400).body(Map.of("success", false, "message", "User is not manageable"));
            case "FORBIDDEN" -> ResponseEntity.status(403).body(Map.of("success", false, "message", "Forbidden"));
            default -> ResponseEntity.status(500).body(Map.of("success", false, "message", "Unknown error"));
        };
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStaff(@PathVariable Long id) {
        String role = CurrentUserService.getCurrentRole().orElse("");
        if (!"admin".equals(role) && !"superadmin".equals(role)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Forbidden"));
        }

        String council = resolveManageCouncil(role);
        if ("admin".equals(role) && council == null) {
            return ResponseEntity.status(400)
                    .body(Map.of("success", false, "message", "Admin has no council assigned"));
        }

        String result = adminStaffService.deleteInternalUser(id, council);
        return switch (result) {
            case "NOT_FOUND" -> ResponseEntity.status(404).body(Map.of("success", false, "message", "User not found"));
            case "NOT_INTERNAL" ->
                ResponseEntity.status(400).body(Map.of("success", false, "message", "User is not deletable"));
            case "FORBIDDEN" -> ResponseEntity.status(403).body(Map.of("success", false, "message", "Forbidden"));
            case "CONFLICT" -> ResponseEntity.status(409)
                    .body(Map.of("success", false, "message", "Cannot delete due to data constraints"));
            case "DELETED" -> ResponseEntity.ok(Map.of("success", true, "message", "Deleted"));
            default -> ResponseEntity.status(500).body(Map.of("success", false, "message", "Unknown error"));
        };
    }

    private String resolveManageCouncil(String role) {
        if ("superadmin".equals(role)) {
            return null;
        }
        return CurrentUserService.getCurrentCouncil().orElse(null);
    }
}
