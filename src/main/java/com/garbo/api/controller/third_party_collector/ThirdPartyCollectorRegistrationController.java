package com.garbo.api.controller.third_party_collector;

import com.garbo.api.dto.thirdparty.ThirdPartyRegistrationRequest;
import com.garbo.api.dto.thirdparty.ThirdPartySetPasswordRequest;
import com.garbo.api.dto.common.ApiResponse;
import com.garbo.core.entity.ThirdPartyCollector;

import com.garbo.core.service.third_party_collector.ThirdPartyCollectorRegistrationService;
import com.garbo.infrastructure.storage.CloudinaryUploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/thirdparty-register")
public class ThirdPartyCollectorRegistrationController {

    private final ThirdPartyCollectorRegistrationService registrationService;
    private final CloudinaryUploadService cloudinaryUploadService;

    public ThirdPartyCollectorRegistrationController(
            ThirdPartyCollectorRegistrationService registrationService,
            CloudinaryUploadService cloudinaryUploadService) {
        this.registrationService = registrationService;
        this.cloudinaryUploadService = cloudinaryUploadService;
    }

    // ─── Public registration endpoints ───

    @GetMapping("/councils")
    public ResponseEntity<ApiResponse<List<String>>> getCouncils() {
        List<String> councils = registrationService.getAvailableCouncils();
        return ResponseEntity.ok(ApiResponse.success(councils));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(
            @RequestBody ThirdPartyRegistrationRequest request) {
        try {
            ThirdPartyCollector collector = registrationService.register(
                    request.getEmpName(),
                    request.getEmail(),
                    request.getPhone(),
                    request.getNIC(),
                    request.getDateOfBirth(),
                    request.getCompany(),
                    request.getContractId(),
                    request.getContractStart(),
                    request.getContractEnd(),
                    request.getDefaultAddress(),
                    request.getNicPhotoUrl(),
                    request.getNicPhotoBackUrl(),
                    request.getAssignedCouncils());

            Map<String, Object> result = Map.of(
                    "empId", collector.getEmpId(),
                    "email", collector.getEmail(),
                    "registrationStatus", collector.getRegistrationStatus().name());

            return ResponseEntity.status(201).body(ApiResponse.success(result));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ex.getMessage(), "VALIDATION_ERROR"));
        }
    }

    @PostMapping("/nic-photo")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadNicPhoto(
            @RequestParam("file") MultipartFile file) {
        try {
            String photoUrl = cloudinaryUploadService.uploadNicPhoto(file);
            return ResponseEntity.ok(ApiResponse.success(Map.of("nicPhotoUrl", photoUrl)));
        } catch (Exception ex) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ex.getMessage(), "UPLOAD_ERROR"));
        }
    }

    @GetMapping("/{empId}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus(@PathVariable Long empId) {
        return registrationService.getRegistrationStatus(empId)
                .map(collector -> {
                    Map<String, Object> data = Map.of(
                            "empId", collector.getEmpId(),
                            "email", collector.getEmail(),
                            "registrationStatus", collector.getRegistrationStatus().name());
                    return ResponseEntity.ok(ApiResponse.success(data));
                })
                .orElse(ResponseEntity.status(404)
                        .body(ApiResponse.error("Third-party collector not found", "NOT_FOUND")));
    }

    @PostMapping("/{empId}/set-password")
    public ResponseEntity<ApiResponse<Map<String, String>>> setPassword(
            @PathVariable Long empId,
            @RequestBody ThirdPartySetPasswordRequest request) {
        try {
            registrationService.setPassword(empId, request.getEmail(), request.getPassword());
            return ResponseEntity.ok(ApiResponse.success(
                    Map.of("message", "Password set successfully. You can now log in.")));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ex.getMessage(), "VALIDATION_ERROR"));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409)
                    .body(ApiResponse.error(ex.getMessage(), "STATE_ERROR"));
        }
    }

    // ─── Test / admin endpoints (public for Postman testing) ───

    @PostMapping("/{empId}/approve")
    public ResponseEntity<ApiResponse<Map<String, Object>>> approve(@PathVariable Long empId) {
        try {
            ThirdPartyCollector collector = registrationService.approve(empId);
            Map<String, Object> data = Map.of(
                    "empId", collector.getEmpId(),
                    "email", collector.getEmail(),
                    "registrationStatus", collector.getRegistrationStatus().name());
            return ResponseEntity.ok(ApiResponse.success(data));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(ex.getMessage(), "NOT_FOUND"));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409)
                    .body(ApiResponse.error(ex.getMessage(), "STATE_ERROR"));
        }
    }

    @PostMapping("/{empId}/reject")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reject(
            @PathVariable Long empId,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String reason = (body != null) ? body.getOrDefault("reason", "") : "";
            ThirdPartyCollector collector = registrationService.reject(empId, reason);
            Map<String, Object> data = Map.of(
                    "empId", collector.getEmpId(),
                    "email", collector.getEmail(),
                    "registrationStatus", collector.getRegistrationStatus().name());
            return ResponseEntity.ok(ApiResponse.success(data));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(ex.getMessage(), "NOT_FOUND"));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409)
                    .body(ApiResponse.error(ex.getMessage(), "STATE_ERROR"));
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<ThirdPartyCollector>>> getPending(
            @RequestParam(required = false) String council) {
        return ResponseEntity.ok(ApiResponse.success(registrationService.getPendingCollectors(council)));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ThirdPartyCollector>>> getActive(
            @RequestParam(required = false) String council) {
        return ResponseEntity.ok(ApiResponse.success(registrationService.getActiveCollectors(council)));
    }

    @GetMapping("/revoked")
    public ResponseEntity<ApiResponse<List<ThirdPartyCollector>>> getRevoked(
            @RequestParam(required = false) String council) {
        return ResponseEntity.ok(ApiResponse.success(registrationService.getRevokedCollectors(council)));
    }

    @GetMapping("/{empId}")
    public ResponseEntity<ApiResponse<ThirdPartyCollector>> getCollector(@PathVariable Long empId) {
        try {
            return ResponseEntity.ok(ApiResponse.success(registrationService.getCollector(empId)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(ex.getMessage(), "NOT_FOUND"));
        }
    }

    @PostMapping("/{empId}/revoke")
    public ResponseEntity<ApiResponse<Map<String, Object>>> revoke(
            @PathVariable Long empId,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String reason = (body != null) ? body.getOrDefault("reason", "Revoked by admin") : "Revoked by admin";
            ThirdPartyCollector collector = registrationService.revoke(empId, reason);
            Map<String, Object> data = Map.of(
                    "empId", collector.getEmpId(),
                    "email", collector.getEmail(),
                    "registrationStatus", collector.getRegistrationStatus().name());
            return ResponseEntity.ok(ApiResponse.success(data));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(ex.getMessage(), "NOT_FOUND"));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409)
                    .body(ApiResponse.error(ex.getMessage(), "STATE_ERROR"));
        }
    }

    @PostMapping("/{empId}/unrevoke")
    public ResponseEntity<ApiResponse<Map<String, Object>>> unrevoke(@PathVariable Long empId) {
        try {
            ThirdPartyCollector collector = registrationService.unrevoke(empId);
            Map<String, Object> data = Map.of(
                    "empId", collector.getEmpId(),
                    "email", collector.getEmail(),
                    "registrationStatus", collector.getRegistrationStatus().name());
            return ResponseEntity.ok(ApiResponse.success(data));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(ex.getMessage(), "NOT_FOUND"));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409)
                    .body(ApiResponse.error(ex.getMessage(), "STATE_ERROR"));
        }
    }

    @PostMapping("/{empId}/hide")
    public ResponseEntity<ApiResponse<Map<String, Object>>> hide(@PathVariable Long empId) {
        try {
            registrationService.hideCollector(empId);
            return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "empId", empId,
                    "message", "Collector hidden from admin list")));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404)
                    .body(ApiResponse.error(ex.getMessage(), "NOT_FOUND"));
        }
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{empId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> delete(@PathVariable Long empId) {
        String result = registrationService.deleteCollector(empId);
        return switch (result) {
            case "DELETED" -> ResponseEntity.ok(ApiResponse.success(Map.of(
                    "empId", empId,
                    "message", "Collector deleted")));
            case "NOT_FOUND" -> ResponseEntity.status(404)
                    .body(ApiResponse.error("Collector not found", "NOT_FOUND"));
            case "CONFLICT" -> ResponseEntity.status(409)
                    .body(ApiResponse.error("Cannot delete due to linked records", "CONFLICT"));
            default -> ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to delete collector", "ERROR"));
        };
    }
}
