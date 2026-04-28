package com.garbo.api.dto.websocket;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload for authentication handshake (client → server).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthPayload {
    
    @JsonProperty("userId")
    private Long userId;
    
}
