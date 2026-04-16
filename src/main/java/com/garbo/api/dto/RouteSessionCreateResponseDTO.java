package com.garbo.api.dto;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RouteSessionCreateResponseDTO {
    public String sessionId;
    public Long userId;
    public String websocketTopic;
    public RouteSessionSnapshotDTO latest;
}
