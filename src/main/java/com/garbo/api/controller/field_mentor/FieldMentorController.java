package com.garbo.api.controller.field_mentor;

import com.garbo.api.dto.common.ApiResponse;

import com.garbo.core.entity.FieldMentor;
import com.garbo.core.service.field_staff.BinService;
import com.garbo.core.service.CurrentUserService;
import com.garbo.core.service.field_staff.FieldMentorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;

// Field mentor flow:
//   Flutter screens under presentation/field_mentor hit these endpoints to
//   list the authenticated mentor's assigned bins.
@RestController
@RequestMapping("/api/fieldmentors")
public class FieldMentorController {

    final private FieldMentorService fieldMentorService;
    final private BinService binService;

    public FieldMentorController(FieldMentorService fieldMentorService, BinService binService) {
        this.fieldMentorService = fieldMentorService;
        this.binService = binService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<FieldMentor>> createFieldMentor(@RequestBody FieldMentor fieldMentor) {
        try {
            String role = CurrentUserService.getCurrentRole().orElse("");
            // Only admin may create FieldMentor
            if (!role.equals("admin")) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Only admin can create field mentors", "FORBIDDEN"));
            }

            // For admin, require admin council and force assignment
            java.util.Optional<String> councilOpt = CurrentUserService.getCurrentCouncil();
            if (councilOpt.isEmpty()) {
                return ResponseEntity.status(400)
                        .body(ApiResponse.error("Admin council not found", "COUNCIL_NOT_FOUND"));
            }

            String adminCouncil = councilOpt.get();
            fieldMentor.setAssignedCouncil(adminCouncil);

            FieldMentor saved = fieldMentorService.saveFieldMentor(fieldMentor);
            return ResponseEntity.ok(ApiResponse.success(saved));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to create field mentor", "CREATE_FAILED"));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FieldMentor>>> getAllFieldMentors() {
        try {
            String role = CurrentUserService.getCurrentRole().orElse("");
            if (role.equals("superadmin")) {
                java.util.List<FieldMentor> all = fieldMentorService.getAll();
                return ResponseEntity.ok(ApiResponse.success(all));
            } else if (role.equals("admin")) {
                java.util.Optional<String> councilOpt = CurrentUserService.getCurrentCouncil();
                if (councilOpt.isEmpty()) {
                    return ResponseEntity.status(400)
                            .body(ApiResponse.error("Admin council not found", "COUNCIL_NOT_FOUND"));
                }
                String council = councilOpt.get();
                java.util.List<FieldMentor> byCouncil = fieldMentorService.findByCouncil(council);
                return ResponseEntity.ok(ApiResponse.success(byCouncil));
            } else {
                return ResponseEntity.status(403).body(ApiResponse.error("Forbidden", "FORBIDDEN"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to fetch field mentors", "FETCH_FAILED"));
        }
    }

    @GetMapping("/{empId}")
    public ResponseEntity<ApiResponse<FieldMentor>> getFieldMentor(@PathVariable Long empId) {
        try {
            FieldMentor fieldMentor = fieldMentorService.getFieldMentor(empId);
            return ResponseEntity.ok(ApiResponse.success(fieldMentor));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage(), "USER_NOT_FOUND"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to fetch field mentor", "INTERNAL_ERROR"));
        }
    }

    // Bins assigned to the authenticated field mentor
    @GetMapping("/me/bins")
    @PreAuthorize("hasRole('FIELD_MENTOR')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAssignedBins() {
        Long empId = CurrentUserService.getCurrentEmpId().orElse(null);
        if (empId == null) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Authenticated field mentor not found", "USER_NOT_FOUND"));
        }
        return ResponseEntity.ok(ApiResponse.success(
                binService.getFormattedBinsForMentor(empId)));
    }

}
