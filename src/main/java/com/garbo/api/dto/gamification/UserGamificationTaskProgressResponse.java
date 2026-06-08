package com.garbo.api.dto.gamification;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserGamificationTaskProgressResponse {
    private Long userId;
    private Long taskId;
    private String taskCode;
    private String taskTitle;
    private String taskDescription;
    private double currentProgress;
    private double targetProgress;
    @JsonProperty("isCompleted")
    private boolean isCompleted;
    private String completedAt;
    private double pointsEarned;
}
