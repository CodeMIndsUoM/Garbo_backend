package com.garbo.api.dto.gamification;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminGamificationTaskUpsertRequest {
    private String code;
    private String title;
    private String description;
    private String roleScope;
    private String taskType;
    private String scoringType;
    private double basePoints;
    private Double targetProgress;
    private Double highPriorityMultiplier;
    private Double mediumPriorityMultiplier;
    private String status;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private Long adminId;
    private Long familyId;
}
