package com.garbo.api.dto.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for handshake confirmation (server → client).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmedPayload {
    
    @JsonProperty("message")
    private String message = "WebSocket connection authenticated and ready for real-time updates";
    
    @JsonProperty("sessionId")
    private String sessionId;
    
}
