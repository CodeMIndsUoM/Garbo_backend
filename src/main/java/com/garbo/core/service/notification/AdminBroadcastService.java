package com.garbo.core.service.notification;

import com.garbo.api.dto.notification.AdminBroadcastRequest;
import com.garbo.core.notification.NotificationContext;
import com.garbo.core.notification.NotificationType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AdminBroadcastService {

    private final NotificationService notificationService;
    private final InternalUserRecipientResolver recipientResolver;

    public AdminBroadcastService(
            NotificationService notificationService,
            InternalUserRecipientResolver recipientResolver
    ) {
        this.notificationService = notificationService;
        this.recipientResolver = recipientResolver;
    }

    @Transactional
    public BroadcastResult sendBroadcast(
            AdminBroadcastRequest request,
            String adminRole,
            String adminCouncil,
            Long adminEmpId
    ) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }

        String title = trimToNull(request.getTitle());
        String body = trimToNull(request.getBody());
        if (title == null) {
            throw new IllegalArgumentException("Title is required");
        }
        if (body == null) {
            throw new IllegalArgumentException("Message body is required");
        }
        if (title.length() > 255) {
            throw new IllegalArgumentException("Title must be 255 characters or fewer");
        }
        if (body.length() > 4000) {
            throw new IllegalArgumentException("Message must be 4000 characters or fewer");
        }

        String councilScope = resolveCouncilScope(adminRole, adminCouncil, request.getCouncil());
        if ("admin".equalsIgnoreCase(adminRole) && (councilScope == null || councilScope.isBlank())) {
            throw new IllegalArgumentException("Admin has no council assigned");
        }

        List<Long> recipients;
        boolean targeted = request.getRecipientIds() != null && !request.getRecipientIds().isEmpty();
        boolean superadmin = "superadmin".equalsIgnoreCase(adminRole);

        if (targeted) {
            recipients = recipientResolver.resolveExplicitRecipients(
                    request.getRecipientIds(),
                    councilScope,
                    superadmin
            );
            if (recipients.isEmpty()) {
                throw new IllegalArgumentException(
                        "No valid internal staff found for the selected user(s). "
                                + "Check that the employee ID belongs to field staff or a bin collector in your council."
                );
            }
        } else {
            recipients = recipientResolver.resolveByAudience(request.getAudience(), councilScope);
            if (recipients.isEmpty()) {
                throw new IllegalArgumentException("No internal staff found for the selected audience");
            }
        }

        String broadcastId = UUID.randomUUID().toString();
        String priority = normalizePriority(request.getPriority());
        String audience = recipientResolver.normalizeAudience(request.getAudience());

        Map<String, Object> data = new HashMap<>();
        data.put("broadcastId", broadcastId);
        data.put("readOnly", true);
        data.put("sentByAdminId", adminEmpId);
        if (councilScope != null) {
            data.put("council", councilScope);
        }
        if (targeted) {
            data.put("targeted", true);
            if (recipients.size() == 1) {
                data.put("recipientEmpId", recipients.get(0));
            }
        } else {
            data.put("audience", audience);
        }

        if (targeted) {
            for (Long recipientId : recipients) {
                Map<String, Object> recipientData = new HashMap<>(data);
                recipientData.put("recipientEmpId", recipientId);

                NotificationContext context = NotificationContext.of(
                        NotificationType.ADMIN_MESSAGE,
                        title,
                        body,
                        recipientData,
                        "admin-message-" + broadcastId + "-" + recipientId,
                        priority
                );
                notificationService.notifyUser(recipientId, NotificationType.ADMIN_MESSAGE, context);
            }
            return new BroadcastResult(broadcastId, recipients.size(), councilScope, "TARGETED");
        }

        NotificationContext context = NotificationContext.of(
                NotificationType.ADMIN_MESSAGE,
                title,
                body,
                data,
                "admin-broadcast-" + broadcastId,
                priority
        );

        notificationService.notifyUsers(recipients, NotificationType.ADMIN_MESSAGE, context);

        return new BroadcastResult(broadcastId, recipients.size(), councilScope, audience);
    }

    @Transactional
    public BroadcastResult sendToUser(
            Long empId,
            String title,
            String body,
            String priority,
            String adminRole,
            String adminCouncil,
            Long adminEmpId
    ) {
        AdminBroadcastRequest request = new AdminBroadcastRequest();
        request.setTitle(title);
        request.setBody(body);
        request.setPriority(priority);
        request.setRecipientIds(List.of(empId));
        return sendBroadcast(request, adminRole, adminCouncil, adminEmpId);
    }

    private String resolveCouncilScope(String adminRole, String adminCouncil, String requestCouncil) {
        if ("admin".equalsIgnoreCase(adminRole)) {
            return adminCouncil != null ? adminCouncil.trim() : null;
        }
        if (requestCouncil != null && !requestCouncil.isBlank()) {
            return requestCouncil.trim();
        }
        return null;
    }

    private String normalizePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return "NORMAL";
        }
        String normalized = priority.trim().toUpperCase();
        return "HIGH".equals(normalized) ? "HIGH" : "NORMAL";
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record BroadcastResult(
            String broadcastId,
            int recipientCount,
            String council,
            String audience
    ) {
    }
}
