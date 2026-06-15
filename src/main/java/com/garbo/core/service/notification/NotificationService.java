package com.garbo.core.service.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.garbo.api.dto.notification.NotificationDto;
import com.garbo.core.entity.DeviceToken;
import com.garbo.core.entity.UserNotification;
import com.garbo.core.notification.NotificationContext;
import com.garbo.core.notification.NotificationType;
import com.garbo.core.repository.DeviceTokenRepository;
import com.garbo.core.repository.UserNotificationRepository;
import com.garbo.infrastructure.push.FcmPushService;
import com.garbo.infrastructure.websocket.NotificationBroadcaster;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class NotificationService {

    private final UserNotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationBroadcaster notificationBroadcaster;
    private final FcmPushService fcmPushService;
    private final ObjectMapper objectMapper;

    public NotificationService(
            UserNotificationRepository notificationRepository,
            DeviceTokenRepository deviceTokenRepository,
            NotificationBroadcaster notificationBroadcaster,
            FcmPushService fcmPushService,
            ObjectMapper objectMapper
    ) {
        this.notificationRepository = notificationRepository;
        this.deviceTokenRepository = deviceTokenRepository;
        this.notificationBroadcaster = notificationBroadcaster;
        this.fcmPushService = fcmPushService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void notifyUser(Long userId, NotificationType type, NotificationContext context) {
        if (userId == null || type == null || context == null) {
            return;
        }
        createAndDeliver(userId, type, context);
    }

    @Transactional
    public void notifyUsers(Collection<Long> userIds, NotificationType type, NotificationContext context) {
        if (userIds == null || userIds.isEmpty() || type == null || context == null) {
            return;
        }
        for (Long userId : userIds) {
            if (userId != null) {
                createAndDeliver(userId, type, context);
            }
        }
    }

    private void createAndDeliver(Long userId, NotificationType type, NotificationContext context) {
        if (context.getSourceEventId() != null && !context.getSourceEventId().isBlank()) {
            Optional<UserNotification> existing = notificationRepository.findByUserIdAndSourceEventId(
                    userId,
                    context.getSourceEventId()
            );
            if (existing.isPresent()) {
                return;
            }
        }

        UserNotification notification = new UserNotification();
        notification.setUserId(userId);
        notification.setType(type.name());
        notification.setTitle(context.getTitle() != null ? context.getTitle() : type.getDefaultTitle());
        notification.setBody(context.getBody() != null ? context.getBody() : type.getDefaultBody());
        String priority = context.getPriority();
        notification.setPriority(
                priority != null && !priority.isBlank() ? priority.trim().toUpperCase() : type.getDefaultPriority()
        );
        notification.setRead(false);
        notification.setSourceEventId(context.getSourceEventId());
        notification.setDataJson(serializeData(context.getData()));

        UserNotification saved = notificationRepository.save(notification);
        Map<String, Object> data = context.getData();
        notificationBroadcaster.broadcastCreated(saved, data);
        fcmPushService.sendNotificationPush(saved, data);
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> listNotifications(
            Long userId,
            int limit,
            String cursor,
            boolean unreadOnly
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        PageRequest page = PageRequest.of(0, safeLimit);

        List<UserNotification> rows;
        if (cursor != null && !cursor.isBlank()) {
            LocalDateTime cursorTime = LocalDateTime.parse(cursor);
            rows = notificationRepository.findByUserIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                    userId,
                    cursorTime,
                    page
            );
        } else if (unreadOnly) {
            rows = notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId, page);
        } else {
            rows = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, page);
        }

        List<NotificationDto> result = new ArrayList<>();
        for (UserNotification row : rows) {
            result.add(NotificationDto.fromEntity(row, parseData(row.getDataJson())));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public boolean markRead(UUID notificationId, Long userId) {
        Optional<UserNotification> row = notificationRepository.findByIdAndUserId(notificationId, userId);
        if (row.isEmpty() || row.get().isRead()) {
            return row.isPresent();
        }
        UserNotification notification = row.get();
        notification.setRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
        return true;
    }

    @Transactional
    public int markAllRead(Long userId) {
        return notificationRepository.markAllRead(userId, LocalDateTime.now());
    }

    @Transactional
    public void registerDeviceToken(Long userId, String token, String platform) {
        if (userId == null || token == null || token.isBlank()) {
            return;
        }
        String normalizedPlatform = platform == null || platform.isBlank()
                ? "android"
                : platform.trim().toLowerCase();

        Optional<DeviceToken> existing = deviceTokenRepository.findByToken(token);
        if (existing.isPresent()) {
            DeviceToken deviceToken = existing.get();
            deviceToken.setUserId(userId);
            deviceToken.setPlatform(normalizedPlatform);
            deviceToken.setLastSeenAt(LocalDateTime.now());
            deviceTokenRepository.save(deviceToken);
            return;
        }

        DeviceToken deviceToken = new DeviceToken();
        deviceToken.setUserId(userId);
        deviceToken.setToken(token);
        deviceToken.setPlatform(normalizedPlatform);
        deviceTokenRepository.save(deviceToken);
    }

    @Transactional
    public void unregisterDeviceToken(Long userId, String token) {
        if (userId == null || token == null || token.isBlank()) {
            return;
        }
        deviceTokenRepository.deleteByUserIdAndToken(userId, token);
    }

    private String serializeData(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize notification data: {}", ex.getMessage());
            return null;
        }
    }

    private Map<String, Object> parseData(String dataJson) {
        return notificationBroadcaster.parseDataJson(dataJson);
    }
}
