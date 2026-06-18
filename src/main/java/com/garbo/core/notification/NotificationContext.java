package com.garbo.core.notification;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class NotificationContext {

    private final String title;
    private final String body;
    private final Map<String, Object> data;
    private final String sourceEventId;
    private final String priority;

    private NotificationContext(
            String title,
            String body,
            Map<String, Object> data,
            String sourceEventId,
            String priority
    ) {
        this.title = title;
        this.body = body;
        this.data = data == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(data));
        this.sourceEventId = sourceEventId;
        this.priority = priority;
    }

    public static NotificationContext of(NotificationType type, Map<String, Object> data, String sourceEventId) {
        return new NotificationContext(
                type.getDefaultTitle(),
                type.getDefaultBody(),
                data,
                sourceEventId,
                null
        );
    }

    public static NotificationContext of(
            NotificationType type,
            String title,
            String body,
            Map<String, Object> data,
            String sourceEventId
    ) {
        return new NotificationContext(title, body, data, sourceEventId, null);
    }

    public static NotificationContext of(
            NotificationType type,
            String title,
            String body,
            Map<String, Object> data,
            String sourceEventId,
            String priority
    ) {
        return new NotificationContext(title, body, data, sourceEventId, priority);
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }

    public String getPriority() {
        return priority;
    }
}
