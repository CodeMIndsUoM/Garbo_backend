package com.garbo.api.dto.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * STOMP payload for admin dashboard bin realtime (/topic/councils/{council}/bins).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouncilBinUpdateMessage {

    @JsonProperty("type")
    private String type;

    @JsonProperty("binId")
    private Long binId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("fillLevel")
    private Integer fillLevel;

    @JsonProperty("council")
    private String council;

    @JsonProperty("changeType")
    private String changeType;

    @JsonProperty("collectionStatus")
    private String collectionStatus;

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("reportId")
    private Long reportId;

    @JsonProperty("notes")
    private String notes;

    @JsonProperty("photoUrl")
    private String photoUrl;

    @JsonProperty("reporterName")
    private String reporterName;

    @JsonProperty("reportedAt")
    private String reportedAt;

    @JsonProperty("hasDiscrepancy")
    private Boolean hasDiscrepancy;

    @JsonProperty("discrepancy")
    private Boolean discrepancy;

    @JsonProperty("previousStatus")
    private String previousStatus;
}
