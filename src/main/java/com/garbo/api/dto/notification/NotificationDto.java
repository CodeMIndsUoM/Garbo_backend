package com.garbo.api.dto.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.garbo.core.entity.UserNotification;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {

    private String id;
    private String title;
    private String body;
    private String type;

    @JsonProperty("read")
    private boolean read;

    private String createdAt;
    private Map<String, Object> data;
    private String priority;

    public static NotificationDto fromEntity(UserNotification entity, Map<String, Object> data) {
        NotificationDto dto = new NotificationDto();
        dto.setId(entity.getId() != null ? entity.getId().toString() : "");
        dto.setTitle(entity.getTitle());
        dto.setBody(entity.getBody());
        dto.setType(entity.getType());
        dto.setRead(entity.isRead());
        dto.setCreatedAt(entity.getCreatedAt() != null
                ? entity.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : null);
        dto.setData(data);
        dto.setPriority(entity.getPriority());
        return dto;
    }
}
