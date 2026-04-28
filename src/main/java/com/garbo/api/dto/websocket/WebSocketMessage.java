package com.garbo.api.dto.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic WebSocket message envelope for all communication.
 * @param <T> Payload type (AUTH, ROUTE_UPDATE, LEADERBOARD_UPDATE, etc.)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage<T> {
    
    @JsonProperty("type")
    private String type;  // AUTH, CONFIRMED, ROUTE_UPDATE, LEADERBOARD_UPDATE, ERROR
    
    @JsonProperty("userId")
    private Long userId;
    
    @JsonProperty("timestamp")
    private long timestamp;
    
    @JsonProperty("payload")
    private T payload;
    
    @JsonProperty("error")
    private String error;
    
    /**
     * Constructor for quick message creation (auto-sets timestamp)
     */
    public WebSocketMessage(String type, Long userId, T payload) {
        this.type = type;
        this.userId = userId;
        this.payload = payload;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Constructor for error messages
     */
    public WebSocketMessage(String type, String error) {
        this.type = type;
        this.error = error;
        this.timestamp = System.currentTimeMillis();
    }
}
