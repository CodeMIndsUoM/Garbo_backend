package com.garbo.api.dto.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskProgressUpdatePayload {

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("binId")
    private Long binId;

    @JsonProperty("totalBinsCollected")
    private Integer totalBinsCollected;

    @JsonProperty("updatedAt")
    private long updatedAt;

    @JsonProperty("tasks")
    private List<TaskProgressItem> tasks;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskProgressItem {
        @JsonProperty("taskId")
        private Long taskId;

        @JsonProperty("taskCode")
        private String taskCode;

        @JsonProperty("taskTitle")
        private String taskTitle;

        @JsonProperty("taskDescription")
        private String taskDescription;

        @JsonProperty("currentProgress")
        private double currentProgress;

        @JsonProperty("targetProgress")
        private double targetProgress;

        @JsonProperty("isCompleted")
        private boolean isCompleted;

        @JsonProperty("completedAt")
        private String completedAt;

        @JsonProperty("pointsEarned")
        private double pointsEarned;
    }
}
