package com.garbo.infrastructure.websocket;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garbo.api.dto.notification.NotificationDto;
import com.garbo.api.dto.websocket.WebSocketMessage;
import com.garbo.core.entity.UserNotification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class NotificationBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketSessionManager sessionManager;
    private final ObjectMapper objectMapper;

    public NotificationBroadcaster(
            SimpMessagingTemplate messagingTemplate,
            WebSocketSessionManager sessionManager,
            ObjectMapper objectMapper
    ) {
        this.messagingTemplate = messagingTemplate;
        this.sessionManager = sessionManager;
        this.objectMapper = objectMapper;
    }

    public void broadcastCreated(UserNotification notification, Map<String, Object> data) {
        if (notification == null || notification.getUserId() == null) {
            return;
        }

        NotificationDto dto = NotificationDto.fromEntity(notification, data);
        Map<String, Object> payload = new HashMap<>();
        payload.put("notification", dto);
        payload.put("type", "NOTIFICATION_CREATED");
        payload.put("updatedAt", System.currentTimeMillis());

        Long userId = notification.getUserId();
        try {
            messagingTemplate.convertAndSend("/topic/users/" + userId + "/notifications", payload);
            sessionManager.sendToUser(
                    userId,
                    new WebSocketMessage<>("NOTIFICATION_CREATED", userId, payload)
            );
            log.debug("Broadcast NOTIFICATION_CREATED to userId={} notificationId={}", userId, dto.getId());
        } catch (Exception ex) {
            log.warn("Failed to broadcast notification to userId={}: {}", userId, ex.getMessage());
        }
    }

    public Map<String, Object> parseDataJson(String dataJson) {
        if (dataJson == null || dataJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(dataJson, new TypeReference<>() {});
        } catch (Exception ex) {
            return Map.of();
        }
    }
}
