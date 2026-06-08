package com.garbo.api.dto.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RouteOptimizeAckPayload {
    private String sessionId;
    private String status;
    private String message;
    private List<Long> selectedBinIds;
    private boolean created;
}
