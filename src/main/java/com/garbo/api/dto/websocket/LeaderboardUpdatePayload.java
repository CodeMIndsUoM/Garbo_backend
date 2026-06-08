package com.garbo.api.dto.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Payload for leaderboard updates (server → client).
 * Contains top collectors with their rankings and scores.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardUpdatePayload {
    
    @JsonProperty("entries")
    private List<LeaderboardEntryDto> entries;
    
    @JsonProperty("updatedAt")
    private long updatedAt;

    @JsonProperty("changedUser")
    private ChangedUserContext changedUser;
    
    /**
     * Single leaderboard entry with rank, name, and score.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeaderboardEntryDto {
        
        @JsonProperty("rank")
        private int rank;
        
        @JsonProperty("userId")
        private Long userId;
        
        @JsonProperty("name")
        private String name;
        
        @JsonProperty("rewardPoints")
        private double rewardPoints;
        
        @JsonProperty("role")
        private String role;  // "COLLECTOR" or "FIELD_MENTOR"
        
        @JsonProperty("rankChangeFromPrevious")
        private Integer rankChangeFromPrevious;  // null if first fetch, +1/-1 if moved up/down
    }

    /**
     * Metadata for the user whose score change triggered this update.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangedUserContext {

        @JsonProperty("userId")
        private Long userId;

        @JsonProperty("role")
        private String role;

        @JsonProperty("taskId")
        private Long taskId;

        @JsonProperty("sourceEventId")
        private String sourceEventId;

        @JsonProperty("previousRank")
        private Integer previousRank;

        @JsonProperty("currentRank")
        private Integer currentRank;

        @JsonProperty("rankDelta")
        private Integer rankDelta;

        @JsonProperty("previousScore")
        private Double previousScore;

        @JsonProperty("currentScore")
        private Double currentScore;

        @JsonProperty("scoreDelta")
        private Double scoreDelta;

        @JsonProperty("enteredTop")
        private boolean enteredTop;

        @JsonProperty("exitedTop")
        private boolean exitedTop;
    }
}
