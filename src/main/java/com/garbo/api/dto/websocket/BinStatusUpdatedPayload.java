package com.garbo.api.dto.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Realtime payload sent when a field staff report/undo changes a bin status.
 * Flutter dashboard listens to this and refreshes assigned-bin cards.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BinStatusUpdatedPayload {

    @JsonProperty("binId")
    private Long binId;

    @JsonProperty("status")
    private String status;

    @JsonProperty("fillLevel")
    private Integer fillLevel;

    @JsonProperty("lastChecked")
    private String lastChecked;

    @JsonProperty("assignedToEmpId")
    private Long assignedToEmpId;

    @JsonProperty("changeType")
    private String changeType;

    @JsonProperty("updatedAt")
    private long updatedAt;
}
