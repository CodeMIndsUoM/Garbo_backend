package com.garbo.api.controller;

import com.garbo.api.dto.common.ApiResponse;
import com.garbo.api.dto.notification.DeviceTokenRequest;
import com.garbo.api.dto.notification.NotificationDto;
import com.garbo.core.service.CurrentUserService;
import com.garbo.core.service.notification.NotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/users/{empId}/notifications")
    public ResponseEntity<ApiResponse<List<NotificationDto>>> listNotifications(
            @PathVariable Long empId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false, defaultValue = "false") boolean unreadOnly
    ) {
        if (!canAccessUser(empId)) {
            return forbidden();
        }
        List<NotificationDto> notifications = notificationService.listNotifications(
                empId,
                limit,
                cursor,
                unreadOnly
        );
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/users/{empId}/notifications/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount(@PathVariable Long empId) {
        if (!canAccessUser(empId)) {
            return forbidden();
        }
        long count = notificationService.unreadCount(empId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("count", count)));
    }

    @PatchMapping("/notifications/{notificationId}/read")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> markRead(
            @PathVariable String notificationId
    ) {
        Long currentUserId = CurrentUserService.getCurrentEmpId().orElse(null);
        if (currentUserId == null) {
            return forbidden();
        }
        UUID id;
        try {
            id = UUID.fromString(notificationId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid notification id", "INVALID_ID"));
        }
        boolean updated = notificationService.markRead(id, currentUserId);
        if (!updated) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Notification not found", "NOT_FOUND"));
        }
        return ResponseEntity.ok(ApiResponse.success(Map.of("read", true)));
    }

    @PatchMapping("/users/{empId}/notifications/read-all")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllRead(@PathVariable Long empId) {
        if (!canAccessUser(empId)) {
            return forbidden();
        }
        int updated = notificationService.markAllRead(empId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("updated", updated)));
    }

    @PostMapping("/users/{empId}/device-tokens")
    public ResponseEntity<ApiResponse<Map<String, String>>> registerDeviceToken(
            @PathVariable Long empId,
            @RequestBody DeviceTokenRequest request
    ) {
        if (!canAccessUser(empId)) {
            return forbidden();
        }
        if (request == null || request.getToken() == null || request.getToken().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Token is required", "VALIDATION"));
        }
        notificationService.registerDeviceToken(empId, request.getToken(), request.getPlatform());
        return ResponseEntity.ok(ApiResponse.success(Map.of("status", "registered")));
    }

    @DeleteMapping("/users/{empId}/device-tokens")
    public ResponseEntity<ApiResponse<Map<String, String>>> unregisterDeviceToken(
            @PathVariable Long empId,
            @RequestBody DeviceTokenRequest request
    ) {
        if (!canAccessUser(empId)) {
            return forbidden();
        }
        if (request == null || request.getToken() == null || request.getToken().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Token is required", "VALIDATION"));
        }
        notificationService.unregisterDeviceToken(empId, request.getToken());
        return ResponseEntity.ok(ApiResponse.success(Map.of("status", "removed")));
    }

    private boolean canAccessUser(Long empId) {
        return CurrentUserService.getCurrentEmpId()
                .map(current -> current.equals(empId))
                .orElse(false);
    }

    private <T> ResponseEntity<ApiResponse<T>> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Forbidden", "FORBIDDEN"));
    }
}
