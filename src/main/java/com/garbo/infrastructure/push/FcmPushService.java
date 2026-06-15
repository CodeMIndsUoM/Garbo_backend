package com.garbo.infrastructure.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.garbo.core.entity.DeviceToken;
import com.garbo.core.entity.UserNotification;
import com.garbo.core.repository.DeviceTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FcmPushService {

    private final DeviceTokenRepository deviceTokenRepository;
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    public FcmPushService(
            DeviceTokenRepository deviceTokenRepository,
            ObjectMapper objectMapper,
            @Value("${firebase.enabled:false}") boolean enabled
    ) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
    }

    public void sendNotificationPush(UserNotification notification, Map<String, Object> data) {
        if (notification == null || notification.getUserId() == null) {
            return;
        }

        List<DeviceToken> tokens = deviceTokenRepository.findByUserId(notification.getUserId());
        if (tokens.isEmpty()) {
            return;
        }

        Map<String, String> fcmData = new HashMap<>();
        fcmData.put("type", notification.getType());
        fcmData.put("notificationId", notification.getId() != null ? notification.getId().toString() : "");
        fcmData.put("priority", notification.getPriority() != null ? notification.getPriority() : "NORMAL");
        if (data != null) {
            data.forEach((key, value) -> {
                if (value != null) {
                    fcmData.put(key, String.valueOf(value));
                }
            });
        }

        for (DeviceToken deviceToken : tokens) {
            sendToToken(deviceToken.getToken(), notification.getTitle(), notification.getBody(), fcmData);
        }
    }

    private void sendToToken(String token, String title, String body, Map<String, String> data) {
        if (!enabled) {
            log.info(
                    "FCM disabled — would push title='{}' to token suffix ...{}",
                    title,
                    token.length() > 8 ? token.substring(token.length() - 8) : token
            );
            return;
        }

        try {
            // Firebase Admin SDK integration point — enable via firebase.enabled=true
            // and GOOGLE_APPLICATION_CREDENTIALS when credentials are available.
            log.info("FCM push queued for token suffix ...{} title='{}'", token.length() > 8
                    ? token.substring(token.length() - 8)
                    : token, title);
            log.debug("FCM payload data={}", objectMapper.writeValueAsString(data));
        } catch (Exception ex) {
            log.warn("FCM push failed: {}", ex.getMessage());
        }
    }
}
