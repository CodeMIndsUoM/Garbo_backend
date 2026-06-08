package com.garbo.api.controller;

import com.garbo.api.dto.common.ApiResponse;
import com.garbo.core.entity.ThirdPartyCollector;
import com.garbo.core.enums.RegistrationStatus;
import com.garbo.core.service.CouncilAccessService;
import com.garbo.core.service.CurrentUserService;
import com.garbo.core.service.third_party_collector.ThirdPartyCollectorRegistrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admins/thirdparty-registrations")
public class AdminThirdPartyRegistrationController {

    private final ThirdPartyCollectorRegistrationService registrationService;
    private final CouncilAccessService councilAccessService;

    public AdminThirdPartyRegistrationController(
            ThirdPartyCollectorRegistrationService registrationService,
            CouncilAccessService councilAccessService) {
        this.registrationService = registrationService;
        this.councilAccessService = councilAccessService;
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<ThirdPartyCollector>>> getPending() {
        String email = CurrentUserService.getCurrentEmail().orElse("");
        String council = councilAccessService.isSuperAdmin(email)
                ? null
                : councilAccessService.resolveCouncilForEmail(email).orElse(null);
        return ResponseEntity.ok(ApiResponse.success(registrationService.getPendingCollectors(council)));
    }

    @PostMapping("/{empId}/approve")
    public ResponseEntity<ApiResponse<Map<String, Object>>> approve(@PathVariable Long empId) {
        try {
            ThirdPartyCollector collector = registrationService.approve(empId);
            return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "empId", collector.getEmpId(),
                    "email", collector.getEmail(),
                    "registrationStatus", collector.getRegistrationStatus().name())));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(ApiResponse.error(ex.getMessage(), "NOT_FOUND"));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(ApiResponse.error(ex.getMessage(), "STATE_ERROR"));
        }
    }

    @PostMapping("/{empId}/reject")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reject(
            @PathVariable Long empId,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String reason = body == null ? "" : body.getOrDefault("reason", "");
            ThirdPartyCollector collector = registrationService.reject(empId, reason);
            return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "empId", collector.getEmpId(),
                    "email", collector.getEmail(),
                    "registrationStatus", collector.getRegistrationStatus().name())));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(404).body(ApiResponse.error(ex.getMessage(), "NOT_FOUND"));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(ApiResponse.error(ex.getMessage(), "STATE_ERROR"));
        }
    }
}
