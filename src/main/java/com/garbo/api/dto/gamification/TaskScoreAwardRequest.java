package com.garbo.api.dto.gamification;

import lombok.Data;

@Data
public class TaskScoreAwardRequest {
    private Long userId;
    private String role;
    private Long taskId;
    private String sourceEventId;
    private String reason;
    private String priorityLevel;
    private Double progressIncrement;
}
