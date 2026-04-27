package com.garbo.api.dto.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BinCollectedPayload {

    @JsonProperty("userId")
    private Long userId;

    @JsonProperty("binId")
    private Long binId;

    @JsonProperty("sessionId")
    private String sessionId;

    @JsonProperty("priority")
    private String priority;

    @JsonProperty("basePoints")
    private Double basePoints;
}
