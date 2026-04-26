package com.garbo.api.dto;

import lombok.Data;

@Data
public class RouteSessionCreateResponseDTO {

    private String sessionId;

    private String websocketTopic;

    private RouteSessionSnapshotDTO snapshot;
}




