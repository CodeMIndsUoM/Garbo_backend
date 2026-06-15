package com.garbo.api.controller;

import com.garbo.api.dto.common.ApiResponse;
import com.garbo.api.dto.notification.AdminBroadcastRequest;
import com.garbo.api.dto.notification.AdminDirectMessageRequest;
import com.garbo.core.service.CurrentUserService;
import com.garbo.core.service.notification.AdminBroadcastService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admins/notifications")
@CrossOrigin(origins = "*")
public class AdminNotificationController {

    private final AdminBroadcastService adminBroadcastService;

    public AdminNotificationController(AdminBroadcastService adminBroadcastService) {
        this.adminBroadcastService = adminBroadcastService;
    }

    /**
     * Send a one-way notification message to internal staff (field mentors / bin collectors).
     * Recipients see it in their mobile inbox; they cannot reply.
     */
    @PostMapping("/broadcast")
    public ResponseEntity<ApiResponse<Map<String, Object>>> broadcast(@RequestBody AdminBroadcastRequest request) {
        String role = CurrentUserService.getCurrentRole().orElse("");
        if (!"admin".equals(role) && !"superadmin".equals(role)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Forbidden", "FORBIDDEN"));
        }

        try {
            AdminBroadcastService.BroadcastResult result = adminBroadcastService.sendBroadcast(
                    request,
                    role,
                    CurrentUserService.getCurrentCouncil().orElse(null),
                    CurrentUserService.getCurrentEmpId().orElse(null)
            );

            return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "broadcastId", result.broadcastId(),
                    "recipientCount", result.recipientCount(),
                    "council", result.council() != null ? result.council() : "",
                    "audience", result.audience()
            )));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ex.getMessage(), "VALIDATION"));
        }
    }

    /**
     * Send a one-way notification to a single internal staff member.
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendToUser(
            @RequestBody AdminDirectMessageRequest request
    ) {
        String role = CurrentUserService.getCurrentRole().orElse("");
        if (!"admin".equals(role) && !"superadmin".equals(role)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Forbidden", "FORBIDDEN"));
        }
        if (request == null || request.getEmpId() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Employee ID is required", "VALIDATION"));
        }

        try {
            AdminBroadcastService.BroadcastResult result = adminBroadcastService.sendToUser(
                    request.getEmpId(),
                    request.getTitle(),
                    request.getBody(),
                    request.getPriority(),
                    role,
                    CurrentUserService.getCurrentCouncil().orElse(null),
                    CurrentUserService.getCurrentEmpId().orElse(null)
            );

            return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "broadcastId", result.broadcastId(),
                    "recipientCount", result.recipientCount(),
                    "recipientEmpId", request.getEmpId(),
                    "council", result.council() != null ? result.council() : "",
                    "audience", "TARGETED"
            )));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ex.getMessage(), "VALIDATION"));
        }
    }
}
